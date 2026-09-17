package net.imglib2.cellpose;

import java.util.function.DoubleConsumer;

import org.apposed.appose.TaskException;

import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.util.AxisInfo;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * A utility wrapper around {@link OmniposeRunner} that allows to process a time
 * series sequentially, and report progress.
 * <p>
 * This exists because normally we cannot send a XYZT or XYCZT image to Python.
 * This class must be considered a WIP, as normally I should find a way to
 * generalize it so that it can handle any runner, not just OmniposeRunner.
 *
 * @author Jean-Yves Tinevez
 */
public class CellposeRunnerWrapper
{

	private final CellposeRunner runner;

	private final DoubleConsumer progressListener;

	public CellposeRunnerWrapper( final CellposeRunner runner, final DoubleConsumer progressListener )
	{
		this.runner = runner;
		this.progressListener = progressListener;
	}

	public < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeOutput< R > run(
			final RandomAccessibleInterval< T > input,
			final AxisInfo axisInfo,
			final R outputType,
			final CellposeParameters params ) throws InterruptedException, TaskException
	{
		if ( axisInfo.T() < 0 )
			throw new IllegalArgumentException( "This wrapper process timepoints sequentially, so the input must have a time axis." );

		// Prepare output holders -> labels
		final long[] labelDims = removeAxisDim( input.dimensionsAsLongArray(), axisInfo.C() );
		final Img< R > outputLabels = Util.getArrayOrCellImgFactory( new FinalDimensions( labelDims ), outputType ).create( labelDims );
		final AxisInfo axisInfoLabels = axisInfo.removeChannelDim();

		// Prepare output holders -> flows
		final Img< UnsignedByteType > outputFlows;
		final AxisInfo axisInfoFlows = axisInfoLabels.insertChannelDim( 2 );
		if ( params.computeFlows )
		{
			final long[] fdims = addAxisDim( labelDims, 2, 3l );
			outputFlows = Util.getArrayOrCellImgFactory( new FinalDimensions( fdims ), new UnsignedByteType() ).create( fdims );
		}
		else
		{
			outputFlows = null;
		}

		final long nT = axisInfo.nTimePoints( input );
		final AxisInfo axisInfoNoT = axisInfo.removeTimeDim();
		for ( long t = 0; t < nT; t++ )
		{
			final RandomAccessibleInterval< T > inputT = Views.hyperSlice( input, axisInfo.T(), t );
			runner.setInput( inputT, axisInfoNoT, outputType );
			runner.run( params );

			// Labels output reslice.
			runner.getOutputLabels( Views.hyperSlice( outputLabels, axisInfoLabels.T(), t ) );

			// Flows output reslice.
			if ( params.computeFlows )
				runner.getOutputFlows( Views.hyperSlice( outputFlows, axisInfoFlows.T(), t ) );

			progressListener.accept( ( double ) ( t + 1 ) / nT );
		}

		return new CellposeOutput< R >( outputLabels, axisInfoLabels, outputFlows, axisInfoFlows );
	}

	private static long[] addAxisDim( final long[] dims, final int d, final long size )
	{
		if ( d < 0 )
			return dims;

		final long[] out = new long[ dims.length + 1 ];
		for ( int i = 0, j = 0; i < out.length; i++ )
		{
			if ( i == d )
				out[ i ] = size;
			else
				out[ i ] = dims[ j++ ];
		}
		return out;
	}

	private static long[] removeAxisDim( final long[] dims, final int d )
	{
		if ( d < 0 )
			return dims;

		final long[] out = new long[ dims.length - 1 ];
		for ( int i = 0, j = 0; i < dims.length; i++ )
		{
			if ( i != d )
				out[ j++ ] = dims[ i ];
		}
		return out;
	}
}
