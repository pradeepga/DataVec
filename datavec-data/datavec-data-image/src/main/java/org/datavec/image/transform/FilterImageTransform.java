/*
 *  * Copyright 2016 Skymind, Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 */
package org.datavec.image.transform;

import java.util.Random;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FrameFilter;
import org.datavec.image.data.ImageWritable;

import static org.bytedeco.javacpp.avutil.*;

/**
 * Filters images using FFmpeg (libavfilter):
 * <a href="https://ffmpeg.org/ffmpeg-filters.html">FFmpeg Filters Documentation</a>.
 *
 * @author saudet
 * @see FFmpegFrameFilter
 */
public class FilterImageTransform extends BaseImageTransform {

    FFmpegFrameFilter filter;

    /** Calls {@code this(filters, width, height, 3)}. */
    public FilterImageTransform(String filters, int width, int height) {
        this(filters, width, height, 3);
    }

    /**
     * Constructs a filtergraph out of the filter specification.
     *
     * @param filters  to use
     * @param width    of the input images
     * @param height   of the input images
     * @param channels of the input images
     */
    public FilterImageTransform(String filters, int width, int height, int channels) {
        super(null);
        int pixelFormat = channels == 1 ? AV_PIX_FMT_GRAY8
                : channels == 3 ? AV_PIX_FMT_BGR24
                : channels == 4 ? AV_PIX_FMT_RGBA : AV_PIX_FMT_NONE;
        if (pixelFormat == AV_PIX_FMT_NONE) {
            throw new IllegalArgumentException("Unsupported number of channels: " + channels);
        }
        try {
            filter = new FFmpegFrameFilter(filters, width, height);
            filter.setPixelFormat(pixelFormat);
            filter.start();
        } catch (FrameFilter.Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ImageWritable transform(ImageWritable image, Random random) {
        if (image == null) {
            return null;
        }
        try {
            filter.push(image.getFrame());
            image = new ImageWritable(filter.pull());
        } catch (FrameFilter.Exception e) {
            throw new RuntimeException(e);
        }
        return image;
    }
}
