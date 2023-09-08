package de.yugata.editor.playback;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_PLAIN;

public class VideoThread extends Thread {


    /**
     * Const. Frame Converter
     */
    public static final OpenCVFrameConverter<Mat> FRAME_CONVERTER = new OpenCVFrameConverter.ToMat();

    /**
     * Const. Point to 50, 50 which will be the text's origin
     */
    public static final Point TEXT_ORIGIN = new Point(50, 50);

    /**
     * Const. Scalar with 255, 255, 255 = r,g,b == white
     */
    public static final Scalar SCALAR_WHITE = new Scalar(255, 255, 255);

    /**
     * The threads internal framegrabber which grabs continuous frames from the provided input-
     */
    private final FFmpegFrameGrabber frameGrabber;

    /**
     * The videoplayer this thread attaches to & provides with the current image to show.
     */
    private final VideoPlayer parent;
    /**
     * Indicates whether the current thread is paused.
     */
    private boolean paused;

    /**
     * Default constructor.
     *
     * @param videoInput the input to read from
     * @param parent     the video player to attach to
     */
    public VideoThread(final String videoInput, final VideoPlayer parent) {
        this.parent = parent;
        this.frameGrabber = new FFmpegFrameGrabber(videoInput);
        frameGrabber.setOption("allowed_extensions", "ALL");
        frameGrabber.setOption("hwaccel", "cuda");
        frameGrabber.setVideoCodecName("hevc_cuvid");
    }

    /**
     * Overwrites the thread's start method, starts the famegrabber to
     */
    @Override
    public void start() {
        try {
            frameGrabber.start();
        } catch (FFmpegFrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
        super.start();
    }


    @Override
    public void run() {
        super.run();
        try {
            /* Grab frames until there are no more or the thread has been interrupted */
            Frame frame;
            while ((frame = frameGrabber.grab()) != null && !interrupted()) {
                if (frame.image == null)
                    continue;

                // Convert the frame to an opencv.core Mat
                final org.opencv.core.Mat coreFrameMat = FRAME_CONVERTER.convertToOrgOpenCvCoreMat(frame);

                // Render the string s to the created mat
                final String s = "Segments: " + parent.currentSegments() + "; Required: " + parent.requiredSegments;
                Imgproc.putText(coreFrameMat, s, TEXT_ORIGIN, FONT_HERSHEY_PLAIN, 4, SCALAR_WHITE);

                // Show the image (convert the previously written to mat to a new frame)
                parent.showImage(FRAME_CONVERTER.convert(coreFrameMat));

                // Wait if the thread is paused, needs to sync the thread.
                synchronized (this) {
                    if (paused) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            } // End frame grabbing
            frameGrabber.stop();
        } catch (FFmpegFrameGrabber.Exception e) {
            e.printStackTrace();
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stopGrabber() {
        try {
            frameGrabber.stop();
        } catch (FFmpegFrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void pause() {
        // Unpause
        if (paused) {
            synchronized (this) {
                this.notify(); //Stop the wait
            }
        }
        this.paused = !paused;
    }

    /**
     * Seek some amount in the stream to a given timestamp
     *
     * @param amount the amount so seek in microseconds
     */
    public void seek(final long amount) {
        try {
            frameGrabber.setTimestamp(frameGrabber.getTimestamp() + amount);
        } catch (FFmpegFrameGrabber.Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public double getCurrentTimeStamp() {
        return frameGrabber.getTimestamp();
    }
}
