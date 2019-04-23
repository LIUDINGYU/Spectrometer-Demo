package com.oceanoptics.spectrometer;

public class ByteRoutines
{
  public static byte getLowByte(short word)
  {
    return (byte)(word & 0xFF);
  }
  
  public static byte getHighByte(short word)
  {
    return (byte)((word & 0xFF00) >> 8);
  }
  
  public static short makeWord(byte high, byte low)
  {
    int i = high << 8 & 0xFF00;
    return (short)(i | low & 0xFF);
  }
  
  public static short makeWordLE(byte high, byte low)
  {
    int i = low << 8 & 0xFF00;
    return (short)(i | high & 0xFF);
  }
  
  public static int makeDWord(short high, short low)
  {
    int h = high << 16 & 0xFFFF0000;
    int l = low & 0xFFFF;
    int mid = h | l;
    return mid;
  }
  
  public static int makeDWordLE(short high, short low)
  {
    int l = low << 16 & 0xFFFF0000;
    int h = high & 0xFFFF;
    int mid = l | h;
    return mid;
  }
  
  public static int makeDWord(byte MSWMSB, byte MSWLSB, byte LSWMSB, byte LSWLSB)
  {
    int mm = MSWMSB << 24 & 0xFF000000;
    int ml = MSWLSB << 16 & 0xFF0000;
    int lm = LSWMSB << 8 & 0xFF00;
    int ll = LSWLSB & 0xFF;
    return mm | ml | lm | ll;
  }
  
  public static int makeDWordLE(byte MSWMSB, byte MSWLSB, byte LSWMSB, byte LSWLSB)
  {
    int ll = LSWLSB << 24 & 0xFF000000;
    int lm = LSWMSB << 16 & 0xFF0000;
    int ml = MSWLSB << 8 & 0xFF00;
    int mm = MSWMSB & 0xFF;
    return ll | lm | ml | mm;
  }
  
  public static short getLowWord(int lng)
  {
    return (short)(lng & 0xFFFF);
  }
  
  public static short getHighWord(int lng)
  {
    return (short)((lng & 0xFFFF0000) >> 16);
  }
  
  public static short byteToShort(byte b)
  {
    return (short)(b & 0xFF);
  }
  
  public static double byteToDouble(byte b)
  {
    return b & 0xFF;
  }
  
  public static int signedByteToUnsignedByte(byte b)
  {
    return 0xFF & b;
  }
  
  public static byte unsignedByteToSignedByte(int i)
  {
    return (byte)i;
  }
  
  public static String byteToHex(byte b)
  {
    int i = b & 0xFF;
    return Integer.toHexString(i);
  }
}
