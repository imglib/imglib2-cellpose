/*-
 * #%L
 * Running Cellpose 3 and 4 from Java with Appose, using ImgLib2 data structure.
 * %%
 * Copyright (C) 2026 Appose developpers
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the ImgLib2 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imglib2.cellpose;

import static net.imglib2.cellpose.CellposeRunner.getTorchInstallSuffix;

import java.io.IOException;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.util.ApposeTaskListener;
import net.imglib2.appose.util.AxisInfo;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * Main class to run Cellpose 3 or Cellpose-SAM from Java, using Appose to
 * manage Python environments and processes, and using ImgLib2 data structures
 * as input and output.
 */
public class Cellpose
{

	/** Enum representing the main operating systems. */
	public enum OperatingSystem
	{
		WINDOWS, LINUX, MACOS, UNKNOWN
	}

	/**
	 * Run Cellpose 3 with the given parameters on the given image, and return
	 * the resulting label image, and optionally the flows. This method uses
	 * UnsignedShortType for the output labels, which is suitable for images
	 * with up to 65k labels. If you expect more than 65k labels in one image,
	 * please use the other cellpose3 method where you can specify the output
	 * label type (UnsignedIntType).
	 * 
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param img
	 *            the input image. X and Y must be the first dimensions.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * @return a {@link CellposeOutput} object containing the label image, and
	 *         optionally the flows image.
	 * @throws BuildException
	 *             if installing and building the Python environment fails.
	 * @throws IOException
	 *             if reading the Python scripts or environment specifications
	 *             fails.
	 * @throws InterruptedException
	 *             if the Python process is interrupted while running.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 */
	public static < T extends RealType< T > & NativeType< T > > CellposeOutput< UnsignedShortType > cellpose3(
			final RandomAccessibleInterval< T > img,
			final AxisInfo axisInfo,
			final Cellpose3Parameters params,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		return cellpose3( img, axisInfo, new UnsignedShortType(), params, listener );
	}

	/**
	 * Run Cellpose 3 with the given parameters on the given image, and return
	 * the resulting label image, and optionally the flows.
	 * 
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param <R>
	 *            the pixel type of the output label image. It can be either
	 *            UnsignedShortType or UnsignedIntType (if the number of labels
	 *            in one image is larger than 65k).
	 * @param img
	 *            the input image. X and Y axes must be at positions 0 and 1
	 *            respectively. If not, a {@link IllegalArgumentException} is
	 *            thrown.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param outputType
	 *            the desired pixel type for the output labels image. It can be
	 *            either UnsignedShortType or UnsignedIntType (if the number of
	 *            labels in one image is larger than 65k).
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * @return a {@link CellposeOutput} object containing the label image, and
	 *         optionally the flows image.
	 * @throws InterruptedException
	 *             if the Python process is interrupted while running.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 * @throws BuildException
	 *             if building the Python environment fails.
	 * @throws IOException
	 *             if there is an error reading the initialization or run
	 *             scripts or the pixi.toml file.
	 */
	public static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeOutput< R > cellpose3(
			final RandomAccessibleInterval< T > img,
			final AxisInfo axisInfo,
			final R outputType,
			final Cellpose3Parameters params,
			final ApposeTaskListener listener ) throws InterruptedException, TaskException, BuildException, IOException
	{
		final CellposeRunner< Cellpose3Parameters > runner = cellpose3Runner( listener, params.torchVersion );
		return run( img, axisInfo, outputType, params, runner );
	}

	/**
	 * Creates a CellposeRunner configured to run Cellpose 3.
	 * <p>
	 * The runner is useful if you want to run Cellpose multiple times on
	 * different images, as it allows to reuse the same Python environment and
	 * the same shared memory placeholders for input and output, which can save
	 * time. In this case, you can create the runner once with this method, and
	 * then call the {@link CellposeRunner#run()} method multiple times.
	 * 
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * @param torchVersion
	 *            the version of PyTorch to use.
	 * @return a CellposeRunner configured to run Cellpose 3.
	 */
	public static CellposeRunner< Cellpose3Parameters > cellpose3Runner( final ApposeTaskListener listener, final String torchVersion )
	{
		final String envName = "cp3-" + getTorchInstallSuffix( torchVersion );
		final String pythonRunScriptPath = "cp3.py";
		return new CellposeRunner<>( CellposeRunner.class.getResource( pythonRunScriptPath ), envName, listener );
	}

	/**
	 * Run Cellpose-SAM with the given parameters on the given image, and return
	 * the resulting label image, and optionally the flows. This method uses
	 * UnsignedShortType for the output labels, which is suitable for images
	 * with up to 65k labels. If you expect more than 65k labels in one image,
	 * please use the other cellpose4 method where you can specify the output
	 * label type (UnsignedIntType).
	 * 
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param img
	 *            the input image. X and Y must be the first dimensions.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * @return a {@link CellposeOutput} object containing the label image, and
	 *         optionally the flows image.
	 * @throws BuildException
	 *             if installing and building the Python environment fails.
	 * @throws IOException
	 *             if reading the Python scripts or environment specifications
	 *             fails.
	 * @throws InterruptedException
	 *             if the Python process is interrupted while running.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 */
	public static < T extends RealType< T > & NativeType< T > > CellposeOutput< UnsignedShortType > cellpose4(
			final RandomAccessibleInterval< T > img,
			final AxisInfo axisInfo,
			final Cellpose4Parameters params,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		return cellpose4( img, axisInfo, new UnsignedShortType(), params, listener );
	}

	/**
	 * Run Cellpose-SAM with the given parameters on the given image, and return
	 * the resulting label image, and optionally the flows.
	 * 
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param <R>
	 *            the pixel type of the output label image. It can be either
	 *            UnsignedShortType or UnsignedIntType (if the number of labels
	 *            in one image is larger than 65k).
	 * @param img
	 *            the input image. X and Y axes must be at positions 0 and 1
	 *            respectively. If not, a {@link IllegalArgumentException} is
	 *            thrown.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param outputType
	 *            the desired pixel type for the output labels image. It can be
	 *            either UnsignedShortType or UnsignedIntType (if the number of
	 *            labels in one image is larger than 65k).
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * 
	 * @return a {@link CellposeOutput} object containing the label image, and
	 *         optionally the flows image.
	 * 
	 * @throws BuildException
	 *             if installing and building the Python environment fails.
	 * @throws IOException
	 *             if reading the Python scripts or environment specifications
	 *             fails.
	 * @throws InterruptedException
	 *             if the Python process is interrupted while running.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 */
	public static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeOutput< R > cellpose4(
			final RandomAccessibleInterval< T > img,
			final AxisInfo axisInfo,
			final R outputType,
			final Cellpose4Parameters params,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		final CellposeRunner< Cellpose4Parameters > runner = cellpose4Runner( listener, params.torchVersion );
		return run( img, axisInfo, outputType, params, runner );
	}

	/**
	 * Creates a CellposeRunner configured to run Cellpose SAM.
	 * <p>
	 * The runner is useful if you want to run Cellpose multiple times on
	 * different images, as it allows to reuse the same Python environment and
	 * the same shared memory placeholders for input and output, which can save
	 * time. In this case, you can create the runner once with this method, and
	 * then call the {@link CellposeRunner#run()} method multiple times.
	 * 
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * @return a CellposeRunner configured to run Cellpose SAM.
	 */
	public static CellposeRunner< Cellpose4Parameters > cellpose4Runner( final ApposeTaskListener listener, final String torchVersion )
	{
		final String envName = "cp4-" + getTorchInstallSuffix( torchVersion );
		final String pythonRunScriptPath = "cp4.py";
		return new CellposeRunner<>( CellposeRunner.class.getResource( pythonRunScriptPath ), envName, listener );
	}

	/**
	 * Core method to run Cellpose 3 or Cellpose-SAM, depending on the
	 * specification of the script and environment to use. To be used by other
	 * methods in this class.
	 * 
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param <R>
	 *            the pixel type of the output label image.
	 * @param input
	 *            the input image. X and Y axes must be at positions 0 and 1
	 *            respectively.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param outputType
	 *            the desired pixel type for the output labels image.
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param runner
	 *            the CellposeRunner to use to run the task.
	 * @return a {@link CellposeOutput} containing the label image, and
	 *         optionally the flows image.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 * @throws InterruptedException
	 *             if the Python process is interrupted while running.
	 * @throws BuildException
	 *             if building the Python environment fails.
	 * @throws IOException
	 *             if there is an error reading the initialization or run
	 *             scripts or the pixi.toml file.
	 */
	private static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R >, CP extends CellposeParameters > CellposeOutput< R > run(
			final RandomAccessibleInterval< T > input,
			final AxisInfo axisInfo,
			final R outputType,
			final CP params,
			final CellposeRunner< CP > runner ) throws InterruptedException, TaskException, BuildException, IOException
	{
		if ( axisInfo.X() != 0 || axisInfo.Y() != 1 )
			throw new IllegalArgumentException( "X and Y axes must be at positions 0 and 1 respectively." );

		// Do we have a 5D image? If yes we process timepoint by timepoint.
		final long nt = axisInfo.nTimePoints( input );
		final long nz = axisInfo.nZ( input );

		runner.init();
		if ( nt > 1 && nz > 1 )
		{
			final CellposeRunnerWrapper wrapper = new CellposeRunnerWrapper( runner, d -> {} );
			return wrapper.run( input, axisInfo, outputType, params );
		}
		else
		{
			// Otherwise process in one go.
			runner.setInput( input, axisInfo, outputType );
			runner.run( params );
			return runner.getOutput();
		}
	}

	/** Prevent instantiation of this utility class. */
	private Cellpose()
	{}
}
