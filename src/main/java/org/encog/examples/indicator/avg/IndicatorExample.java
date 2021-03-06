package org.encog.examples.indicator.avg;

import java.io.File;

import org.encog.cloud.indicator.IndicatorConnectionListener;
import org.encog.cloud.indicator.IndicatorFactory;
import org.encog.cloud.indicator.IndicatorListener;
import org.encog.cloud.indicator.server.IndicatorLink;
import org.encog.cloud.indicator.server.IndicatorServer;
import org.encog.ml.MLMethod;
import org.encog.ml.MLRegression;
import org.encog.ml.MLResettable;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.factory.MLMethodFactory;
import org.encog.ml.factory.MLTrainFactory;
import org.encog.ml.train.MLTrain;
import org.encog.ml.train.strategy.RequiredImprovementStrategy;
import org.encog.neural.networks.training.propagation.manhattan.ManhattanPropagation;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.util.simple.EncogUtility;

/**
 * This example shows how to create an indicator based on two existing
 * NinjaTrader indicators.  For more information on this example, visit
 * the following URL.
 * 
 * http://www.heatonresearch.com/wiki/Neural_Network_Indicator_for_NinjaTrader_with_Java
 *
 */
public class IndicatorExample implements IndicatorConnectionListener {

	/**
	 * The port to use.
	 */
	public static final int PORT = 5128;
	
	/**
	 * The path to store files at.
	 */
	private File path;

	/**
	 * Construct the indicator example.
	 * @param thePath The path to store data files at.
	 */
	public IndicatorExample(File thePath) {
		this.path = thePath;
	}

	/**
	 * Perform the training option.
	 */
	public void train() {
		// first, create the machine learning method
		MLMethodFactory methodFactory = new MLMethodFactory();		
		MLMethod method = methodFactory.create(Config.METHOD_TYPE, Config.METHOD_ARCHITECTURE, Config.INPUT_WINDOW, 1);
		
		// second, create the data set	
		File filename = new File(this.path,Config.FILENAME_TRAIN);
		MLDataSet dataSet = EncogUtility.loadEGB2Memory(filename);
		
		// third, create the trainer
		MLTrainFactory trainFactory = new MLTrainFactory();	
		MLTrain train = trainFactory.create(method,dataSet,Config.TRAIN_TYPE,Config.TRAIN_PARAMS);				
		// reset if improve is less than 1% over 5 cycles
		if( method instanceof MLResettable && !(train instanceof ManhattanPropagation) ) {
			train.addStrategy(new RequiredImprovementStrategy(500));
		}

		// fourth, train and evaluate.
		EncogUtility.trainToError(train, Config.TARGET_ERROR);
		method = train.getMethod();	
		EncogDirectoryPersistence.saveObject(new File(this.path,Config.METHOD_NAME), method);
		
		// finally, write out what we did
		System.out.println("Machine Learning Type: " + Config.METHOD_TYPE);
		System.out.println("Machine Learning Architecture: " + Config.METHOD_ARCHITECTURE);

		System.out.println("Training Method: " + Config.TRAIN_TYPE);
		System.out.println("Training Args: " + Config.TRAIN_PARAMS);
	}
	
	/**
	 * Perform the calibrate option.
	 */
	private void calibrate() {
		GenerateTraining gen = new GenerateTraining(this.path);
		gen.calibrate();
	}

	/**
	 * Perform the generate option.
	 */
	private void generate() {
		System.out.println("Generating training data... please wait...");
		GenerateTraining gen = new GenerateTraining(this.path);
		gen.generate();
		System.out.println("Training data has been generated.");
	}

	/**
	 * Perform the clear option.
	 */
	private void clear() {
		File[] list = this.path.listFiles();

		for (File file : list) {
			String fn = file.getName();
			if (fn.startsWith("collected") && fn.endsWith(".csv")) {
				file.delete();
			}
		}
		
		System.out.println("Directory cleared of captured financial data.");

	}

	/**
	 * Run the indicator in either collection or indicator mode.
	 * @param collectMode True to run the indicator in collection mode,
	 * false otherwise.
	 */
	public void run(final boolean collectMode) {
		final MLRegression method;

		if (collectMode) {
			method = null;
			System.out.println("Ready to collect data from remote indicator.");
		} else {
			method = (MLRegression)EncogDirectoryPersistence.loadObject(new File(this.path,Config.METHOD_NAME));
			System.out.println("Indicator ready.");
		}

		System.out.println("Waiting for connections on port " + PORT);

		IndicatorServer server = new IndicatorServer();
		server.addListener(this);

		server.addIndicatorFactory(new IndicatorFactory() {
			@Override
			public String getName() {
				return "MYIND";
			}

			@Override
			public IndicatorListener create() {
				return new MyInd(method, path);
			}
		});

		server.start();

	}

	/**
	 * The program entry point.
	 * @param args The arguments to run the program with.
	 */
	public static void main(String[] args) {		

		if (args.length != 2) {
			System.out
					.println("Usage: IndicatorExample [clear/collect/generate/train/run] [work path]");
		} else {
			IndicatorExample program = new IndicatorExample(new File(args[1]));
			if (args[0].equalsIgnoreCase("collect")) {
				program.run(true);
			} else if (args[0].equalsIgnoreCase("train")) {
				program.train();
			} else if (args[0].equalsIgnoreCase("run")) {
				program.run(false);
			} else if (args[0].equalsIgnoreCase("clear")) {
				program.clear();
			} else if (args[0].equalsIgnoreCase("generate")) {
				program.generate();
			} else if (args[0].equalsIgnoreCase("calibrate")) {
				program.calibrate();
			}
		}
	}

	/**
	 * Called when a connection is made by a trading platform.
	 */
	@Override
	public void notifyConnections(IndicatorLink link, boolean hasOpened) {
		if (hasOpened) {
			System.out.println("Connection from " + link.getSocket().toString()
					+ " established.");
		} else if (!hasOpened) {
			System.out.println("Connection from " + link.getSocket().toString()
					+ " terminated.");
		}
	}
}
