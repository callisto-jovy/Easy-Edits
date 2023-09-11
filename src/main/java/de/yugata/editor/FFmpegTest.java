package de.yugata.editor;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.librealsense.frame;

public class FFmpegTest {


    public static void main(String[] args) throws FrameGrabber.Exception, FrameRecorder.Exception, FrameFilter.Exception {
        FFmpegLogCallback.set();

        final FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber("D:\\out\\edits\\The Shining\\The Shining (1980) (2160p BluRay x265 10bit HDR Tigole).mkv_edit_2.mkv");
        frameGrabber.setOption("allowed_extensions", "ALL");
        frameGrabber.setOption("hwaccel", "cuda");
      //  frameGrabber.setVideoCodecName("hevc_cuvid");

        frameGrabber.start();


        // Video filter interpolate
        final FFmpegFrameFilter interpolateFilter = new FFmpegFrameFilter("minterpolate=fps=60,tblend=all_mode=average", frameGrabber.getImageWidth(), frameGrabber.getImageHeight());
        interpolateFilter.setFrameRate(frameGrabber.getFrameRate());
        interpolateFilter.setPixelFormat(frameGrabber.getPixelFormat());
        interpolateFilter.setSampleFormat(frameGrabber.getSampleFormat());
        interpolateFilter.start();

        /*
        final FFmpegFrameFilter colorFilter = new FFmpegFrameFilter("setpts=N,eq=brightness=0.1:saturation=3:gamma=1", frameGrabber.getImageWidth(), frameGrabber.getImageHeight());
        colorFilter.setFrameRate(frameGrabber.getFrameRate());
        colorFilter.setPixelFormat(frameGrabber.getPixelFormat());
        colorFilter.setSampleFormat(frameGrabber.getSampleFormat());
        colorFilter.start();

         */


        final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder("test_video.mkv", frameGrabber.getImageWidth(), frameGrabber.getImageHeight(), 2);
        recorder.setFormat("matroska");
        recorder.setVideoOption("color_range", "tv");
        recorder.setVideoOption("colorspace", "bt2020nc");
        recorder.setVideoOption("color_primaries", "bt2020");
        recorder.setVideoOption("color_trc", "smpte2084");
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(frameGrabber.getFrameRate());
        //      recorder.setSampleRate(sampleRate);
        final int bitrate = 80 * frameGrabber.getImageWidth() * frameGrabber.getImageHeight();
        recorder.setVideoBitrate(Math.max(bitrate, frameGrabber.getVideoBitrate())); // max bitrate
        recorder.setVideoQuality(0);
        recorder.setVideoCodecName("h264_nvenc"); // Hardware-accelerated encoding.
        recorder.start();


        final FFmpegFrameFilter[] filters = new FFmpegFrameFilter[] {interpolateFilter};
        Frame frame;
        while ((frame = frameGrabber.grabImage()) != null) {



/*

            interpolateFilter.push(frame);

            while ((frame = interpolateFilter.pull()) != null) {
                System.out.println("Recording");


                colorFilter.push(frame);

                while ((frame = colorFilter.pull()) != null) {
                    recorder.record(frame);
                }
            }

 */



            pushToFilters(frame, recorder, filters);
        }

     //   colorFilter.close();
        interpolateFilter.close();
        recorder.close();
        frameGrabber.close();
    }


    private static void pushToFilters(final Frame frame, final FFmpegFrameRecorder recorder, final FFmpegFrameFilter... filters) {
        try {
            System.out.println(filters);
            // Feed the frame to the first filter
            filters[0].push(frame);

            //
            for (int i = 1; i < filters.length; i++) {
                // Pull frames from predecessor
                final FFmpegFrameFilter predecessor = filters[i - 1];
                Frame processedFrame;

                while ((processedFrame = predecessor.pull()) != null) {
                    filters[i].push(processedFrame);
                }
            }

            System.out.println(filters);
            // Grab the frames from the last filter...

            System.out.println(filters.length);

            final FFmpegFrameFilter finalFilter = filters[filters.length - 1];

            Frame processedFrame;

            while ((processedFrame = finalFilter.pull()) != null) {
                System.out.println("Recording");
                recorder.record(processedFrame);
            }

        } catch (FFmpegFrameRecorder.Exception | FrameFilter.Exception e) {
            e.printStackTrace();
        }
    }

}
