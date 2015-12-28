package it.prz.jmatrw4spark;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import it.prz.jmatrw.JMATReader;
import it.prz.jmatrw.JMATValue;


public class JMATFileRecordReader extends RecordReader<LongWritable, DoubleWritable> {

	private JMATReader _matReader = null;
	private long lBlockStart = 0;
	private long lBlockLength = 0;
	private long lBlockEnd = 0;
	private long lBlockCurPos = 0;
	private LongWritable key;
	private DoubleWritable value;
	
	public void initialize(InputSplit baseSplit, TaskAttemptContext ctx) throws IOException, InterruptedException {
		Configuration cfg = ctx.getConfiguration();
		
		FileSplit fileSplit = (FileSplit) baseSplit;
		Path filePath = fileSplit.getPath();

		FileSystem fs = filePath.getFileSystem(cfg);
		FSDataInputStream is = fs.open(fileSplit.getPath());
		
		//Initialise the block boundaries.
		lBlockStart = fileSplit.getStart();
		lBlockLength = fileSplit.getLength();
		lBlockEnd = lBlockStart + lBlockLength;
		lBlockCurPos = lBlockStart;
		
		//Initialise the object to read the *.mat file.
		_matReader = new JMATReader(is);
	}//EndMethod.

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		if (key == null) key = new LongWritable(0);
		if (value == null) value = new DoubleWritable(0);
		
		if (lBlockCurPos == lBlockEnd) return false;
		if (_matReader.hasNext() == false) return false;
		
		JMATValue matValue = _matReader.next();
		lBlockCurPos = matValue.indexPosition;
		key.set(lBlockCurPos);
		value.set(matValue.value);
		
		return true;
	}//EndMethod.

	@Override
	public LongWritable getCurrentKey() throws IOException, InterruptedException {
		return key;
	}//EndMethod.

	@Override
	public DoubleWritable getCurrentValue() throws IOException, InterruptedException {
		return value;
	}//EndMethod.

	@Override
	public float getProgress() throws IOException, InterruptedException {
		if (lBlockLength == 0) return 0;
		
		float fProgress = (lBlockCurPos - lBlockStart) / (float) lBlockLength;
		return Math.min(1.0f, fProgress);
	}//EndMethod.

	@Override
	public void close() throws IOException {
		if (_matReader != null)
			_matReader.close();
	}//EndMethod.
	
}//EndClass.
