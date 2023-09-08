package de.yugata.editor.playback;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_DUPLEX;
import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_PLAIN;

public class VideoThread extends Thread {

    private final FFmpegFrameGrabber frameGrabber;

    private final VideoPlayer parent;

    private boolean paused;

    public VideoThread(final String videoInput, final VideoPlayer parent) {
        this.parent = parent;
        this.frameGrabber = new FFmpegFrameGrabber(videoInput);
        frameGrabber.setOption("allowed_extensions", "ALL");
        frameGrabber.setOption("hwaccel", "cuda");
        frameGrabber.setVideoCodecName("hevc_cuvid");

    }

    @Override
    public void start() {
        try {
            frameGrabber.start();
        } catch (FFmpegFrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
        super.start();
    }


    private final OpenCVFrameConverter<Mat> javaCVConv = new OpenCVFrameConverter.ToMat();
    private final Point textOrigin = new Point(50, 50);
    private final Scalar scalarWhite = new Scalar(255, 255, 255);

    @Override
    public void run() {
        super.run();
        try {
            Frame frame;
            while ((frame = frameGrabber.grabImage()) != null && !interrupted()) {

                final org.opencv.core.Mat coreFrameMat = javaCVConv.convertToOrgOpenCvCoreMat(frame);

                Imgproc.putText(coreFrameMat, "Segments: " + parent.timeStamps.size(), textOrigin, FONT_HERSHEY_PLAIN, 4, scalarWhite);

                parent.showImage(javaCVConv.convert(coreFrameMat));

                synchronized (this) {
                    if (paused) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

            }
            frameGrabber.stop();
        } catch (FFmpegFrameGrabber.Exception e) {
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
                this.notify();
            }
        }
        this.paused = !paused;
    }

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
