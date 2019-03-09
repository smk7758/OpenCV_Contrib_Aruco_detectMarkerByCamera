package com.github.smk7758.OpenCV_Contrib_Aruco_0;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;

public class MarkerDetectorService extends ScheduledService<Image> {
	final Dictionary dictionary;
	// Mat inputImage;
	// List<Mat> corners;
	// Mat markerIds;

	static final int provisionalCameraNumber = 0;

	VideoCapture vc = null;
	int cameraNumber = 0;
	final Path filePath = Paths.get("S:\\program\\CameraCaliblation\\CameraCalibration_2018-12-31.xml");
	final Map<String, Mat> calibrationMats = MatIO.loadMat(filePath);
	Mat cameraMatrix = calibrationMats.get("CameraMatrix"),
			distortionCoefficients = calibrationMats.get("DistortionCoefficients");

	public MarkerDetectorService(Dictionary dictionary, int cameraNumber) {
		this.dictionary = dictionary;
		this.cameraNumber = cameraNumber;
		initialize();

		this.setOnCancelled(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				vc.release();
			}
		});
	}

	public MarkerDetectorService(Dictionary dictionary) {
		this(dictionary, provisionalCameraNumber);
	}

	public void initialize() {
		vc = new VideoCapture();
		vc.open(cameraNumber);
	}

	@Override
	public void reset() {
		super.reset();

		initialize();
	}

	@Override
	public boolean cancel() {
		Main.printDebug("Service is stopped.");
		return super.cancel();
	}

	@Override
	protected Task<Image> createTask() {
		return new Task<Image>() {
			@Override
			protected Image call() throws Exception {
				if (!vc.isOpened()) {
					System.err.println("VC is not opened.");
					this.cancel();
					return null;
				}

				Mat inputImage = new Mat();

				if (!vc.read(inputImage) || inputImage == null) {
					System.err.println("Cannot load camera image.");
					this.cancel();
					return null;
				}

				List<Mat> corners = new ArrayList<>(); // Matにcornerが(x, y)で4隅の点があり、Listはマーカーの数。
				Mat markerIds = new Mat();
				// DetectorParameters parameters = DetectorParameters.create();
				Aruco.detectMarkers(inputImage, dictionary, corners, markerIds);

				Aruco.drawDetectedMarkers(inputImage, corners, markerIds);

				// System.out.println("------");
				// corners.forEach(mat -> System.out.println(mat.dump()));

				System.out.println("A"); // TODO
				for (Mat corner : corners) {
					Mat rotationMatrix = new Mat(), translationVectors = new Mat(); // 受け取る
					List<Mat> corner_ = new ArrayList<>();
					corner_.add(corner);
					System.out.println("A'"); // TODO
					Aruco.estimatePoseSingleMarkers(corner_, 0.05f, cameraMatrix, distortionCoefficients,
							rotationMatrix, translationVectors);
					System.out.println("B"); // TODO

					for (int i = 0; i < markerIds.size().height; i++) { // TODO
						Aruco.drawAxis(inputImage, cameraMatrix, distortionCoefficients,
								rotationMatrix, translationVectors, 0.1f);
					}
					System.out.println("C"); // TODO
				}

				return convertMatToImage(inputImage);
			}
		};
	}

	private Image convertMatToImage(Mat inputImage) {
		MatOfByte byte_mat = new MatOfByte();
		Imgcodecs.imencode(".bmp", inputImage, byte_mat);

		return new Image(new ByteArrayInputStream(byte_mat.toArray()));
	}
}
