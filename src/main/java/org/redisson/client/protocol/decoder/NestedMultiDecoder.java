/**
 * Copyright 2014 Nikita Koksharov, Nickolay Borbit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson.client.protocol.decoder;

import java.io.IOException;
import java.util.List;

import org.redisson.client.handler.State;

import io.netty.buffer.ByteBuf;

public class NestedMultiDecoder<T> implements MultiDecoder<Object> {

    public static class DecoderState {

        int decoderIndex;
        
        int flipDecoderIndex;

        public DecoderState() {
        }

        public int getDecoderIndex() {
            return decoderIndex;
        }
        public void resetDecoderIndex() {
            decoderIndex = 0;
        }
        public void incDecoderIndex() {
            decoderIndex++;
        }
        
        public int getFlipDecoderIndex() {
            return flipDecoderIndex;
        }
        public void resetFlipDecoderIndex() {
            flipDecoderIndex = 0;
        }
        public void incFlipDecoderIndex() {
            flipDecoderIndex++;
        }

    }

    protected final MultiDecoder<Object> firstDecoder;
    protected final MultiDecoder<Object> secondDecoder;
    private MultiDecoder<Object> thirdDecoder;

    public NestedMultiDecoder(MultiDecoder<Object> firstDecoder, MultiDecoder<Object> secondDecoder) {
        this.firstDecoder = firstDecoder;
        this.secondDecoder = secondDecoder;
    }
    
    public NestedMultiDecoder(MultiDecoder<Object> firstDecoder, MultiDecoder<Object> secondDecoder, MultiDecoder<Object> thirdDecoder) {
        this.firstDecoder = firstDecoder;
        this.secondDecoder = secondDecoder;
        this.thirdDecoder = thirdDecoder;
    }


    @Override
    public Object decode(ByteBuf buf, State state) throws IOException {
        DecoderState ds = getDecoder(state);

        MultiDecoder<?> decoder = null;
        if (ds.getFlipDecoderIndex() == 2) {
            decoder = firstDecoder;
        }
        if (ds.getFlipDecoderIndex() == 1) {
            decoder = secondDecoder;
        }

        return decoder.decode(buf, state);
    }

    @Override
    public boolean isApplicable(int paramNum, State state) {
        DecoderState ds = getDecoder(state);
        if (paramNum == 0) {
            ds.incFlipDecoderIndex();
            ds.resetDecoderIndex();
        }
        // used only with thirdDecoder
        if (ds.getFlipDecoderIndex() == 3) {
            ds.resetFlipDecoderIndex();
            ds.incFlipDecoderIndex();
        }

        MultiDecoder<?> decoder = null;
        if (ds.getFlipDecoderIndex() == 2) {
            decoder = firstDecoder;
        }
        if (ds.getFlipDecoderIndex() == 1) {
            decoder = secondDecoder;
        }
        
        return decoder.isApplicable(paramNum, state);
    }

    protected final DecoderState getDecoder(State state) {
        DecoderState ds = state.getDecoderState();
        if (ds == null) {
            ds = new DecoderState();
            state.setDecoderState(ds);
        }
        return ds;
    }

    @Override
    public Object decode(List<Object> parts, State state) {
        if (parts.isEmpty() && state.getDecoderState() == null) {
            MultiDecoder<?> decoder = secondDecoder;
            if (thirdDecoder != null) {
                decoder = thirdDecoder;
            }
            return decoder.decode(parts, state);
        }

        DecoderState ds = getDecoder(state);
        if (parts.isEmpty()) {
            ds.resetDecoderIndex();
        }
        
        ds.incDecoderIndex();
        MultiDecoder<?> decoder = null;
        if (ds.getDecoderIndex() == 1) {
            decoder = firstDecoder;
        }
        if (ds.getDecoderIndex() == 2) {
            decoder = secondDecoder;
        }
        if (ds.getDecoderIndex() == 3) {
            decoder = thirdDecoder;
        }

        return decoder.decode(parts, state);
    }

}
