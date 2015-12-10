/*
 * (C) Copyright 2015 Donato Pirozzi <donatopirozzi@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 3 which accompanies this distribution (See the COPYING.LESSER
 * file at the top-level directory of this distribution.), and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Donato Pirozzi
 */

package it.jmatrw;

import java.io.IOException;
import java.io.InputStream;

import it.jmatrw.JMATData.DataType;
import it.jmatrw.io.DataElement;
import it.jmatrw.io.JMatInput;
import it.jmatrw.io.DataElement.DEType;
import it.jmatrw.matdatatypes.MLArrayTypeClass;
import it.jmatrw.matdatatypes.MLDataType;
import it.jmatrwio.utils.ByteArray.ByteArrayOrder;

/**
 * 
 * @author Donato Pirozzi - donatopirozzi@gmail.com
 */
public class JMATReader {

	private JMatInput _reader = null;
	private JMATData mldata = new JMATData();
	
	public JMATReader(InputStream is) { _reader = new JMatInput(is); }//EndConstructor.
	
	public JMATData read() throws IOException {
		readHeader();
		
		readElementData();
		
		return mldata;
	}//EndMethod.
	
	private int readElementData() throws IOException {
		int dataType = (int) _reader.readBytes(4).getUInt32();
		int iNumOfBytes = (int) _reader.readBytes(4).getUInt32();
		
		if (dataType == MLDataType.miMATRIX.value) {
			//Read the ArrayFlag Subelement.
			DataElement _arrFlag = _reader.readDataElementHeader();
			assert (_arrFlag.dataType == MLDataType.miUINT32);
			assert (_arrFlag.numOfBytesBody == 8); //size is 8 bytes = [miUINT32 * 2]
			
			byte[] bArrFlag = _reader.readBytes(MLDataType.miUINT32.bytes).arrayEndian();
			_reader.readBytes(MLDataType.miUINT32.bytes); //Undefined 4 bytes in the flag data.
			byte lflag = bArrFlag[2];
			byte lclass = bArrFlag[3];
			MLArrayTypeClass arrayTypeClass = MLArrayTypeClass.dataTypeFromValue(lclass);
			
			//Dimensions Array Subelement.
            DataElement _dimArray = _reader.readDataElementHeader();
			assert (_dimArray.dataType == MLDataType.miINT32);
			int numOfArrayDims = _dimArray.numOfBytesBody / 4;
			assert (numOfArrayDims >= 2);//A Matlab array has at least two dimensions.
			
			//TODO: it works only for array of 2 dimensions.
			_reader.readBytes(4).getUInt32();
			_reader.readBytes(4).getUInt32();
			
			//Array name.
			DataElement _arrName = _reader.readDataElementHeader();
			assert (_dimArray.dataType == MLDataType.miINT8);
			
			//TODO: probably it can be moved inside the readDataElementHeader()
			if (_arrName.dataElementType == DEType.STANDARD && _arrName.numOfBytesBody % 8 != 0)
				_arrName.numOfBytesBody = (_arrName.numOfBytesBody / 8 + 1) * 8;
						
			byte[] _bArrName = _reader.readBytes(_arrName.numOfBytesBody).arrayEndian();
			mldata.dataName = new StringBuilder(new String(_bArrName)).reverse().toString().trim();
			
			//Read the numbers.
			DataElement _arrCells = _reader.readDataElementHeader();
			
			double[] arrValues = null;
			if (_arrCells.dataType == MLDataType.miDOUBLE) 
				arrValues = new double[_arrCells.numOfBytesBody / _arrCells.dataType.bytes];
				
			int iRedBytes = 0;
			int index = 0;
			while (iRedBytes < _arrCells.numOfBytesBody) {
				//Read the next number.
				double value = _reader.readBytes(_arrCells.dataType.bytes).getDouble();
				arrValues[index] = value;
				iRedBytes += _arrCells.dataType.bytes;
				index++;
			}
			
			mldata.dataType = DataType.ARRAY_DOUBLE;
			mldata.dataValue = arrValues;
			
		} else if (dataType == MLDataType.miUINT32.value) {
			/*int iNumBytesToRead = MLDataType.dataTypeFromValue(dataType).bytes;
			ByteBuffer buffer = _reader.readBytes(iNumBytesToRead);
 			long value = buffer.getInt();
			System.out.printf("Value: %d\n", value);
			return iNumBytesToRead;*/
		}
	
		return 0;
	}//EndMethod.
	
	private void readHeader() throws IOException {
		//Header Text Field.
		byte[] bArr = _reader.readBytes(116).arrayEndian();
		mldata.header = new String(bArr);
		
		//Header Subsystem Data Offset Field.
		_reader.readBytes(8);
		
		//Header Flag Fields.
		bArr = _reader.readBytes(4).arrayEndian();
		mldata.version = bArr[0] << 8 | bArr[1];
		String endianIndicator = new String( new byte[] { bArr[2], bArr[3] }); //TODO: This I think can be replaced with new String();
		if (endianIndicator.equalsIgnoreCase("IM")) _reader.setEndianEncoding(ByteArrayOrder.LITTLE_ENDIAN);
	}//EndMethod.
	
}//EndClass.
