package fr.repele.helpers;

import java.nio.ByteBuffer;

import SecuGen.Driver.DeviceInfoParam;
import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGDeviceInfoParam;
import SecuGen.FDxSDKPro.SGFDxErrorCode;
import SecuGen.FDxSDKPro.SGFDxSecurityLevel;
import SecuGen.FDxSDKPro.SGFingerInfo;
import SecuGen.FDxSDKPro.SGFingerPosition;
import android.R.color;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

public final class FDxSDKHelper {

	private static FDxSDKHelper mFDxSDKHelper = null;

	///////////////////////////////////////////////
	//		Vars pour le lecteur d'empreintes	///
	///////////////////////////////////////////////

	private static String TAG = "Bachtech";
	private static int[] grayBuffer;
	private static Bitmap grayBitmap;
	public static int[] mMaxTemplateSize;
	public static PendingIntent mPermissionIntent;
	public static JSGFPLib sgfplib; // Objet à utiliser pour interagir avec le lecteur d'empreintes
	private static Context context;
	private static int mImageWidth;
	private static int mImageHeight;
	private static SGDeviceInfoParam deviceInfo;


	///////////////////////////////////////////////
	///////////////////////////////////////////////

	private FDxSDKHelper(Context context){

		////////////////////////////////////////////////////////////////////
		//					Initialisation de lecteur d'empreites		  //
		////////////////////////////////////////////////////////////////////

		FDxSDKHelper.context = context;
		deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
		mImageWidth = deviceInfo.imageWidth;
		mImageHeight= deviceInfo.imageHeight;
		grayBuffer = new int[JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES*JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES];
		for (int i=0; i<grayBuffer.length; ++i)
			grayBuffer[i] = android.graphics.Color.GRAY;
		grayBitmap = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES, Bitmap.Config.ARGB_8888);
		grayBitmap.setPixels(grayBuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES); 
		//mImageViewFingerprint.setImageBitmap(grayBitmap);

		int[] sintbuffer = new int[(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2)*(JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2)];
		for (int i=0; i<sintbuffer.length; ++i)
			sintbuffer[i] = android.graphics.Color.GRAY;
		Bitmap sb = Bitmap.createBitmap(JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2, Bitmap.Config.ARGB_8888);
		sb.setPixels(sintbuffer, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, 0, 0, JSGFPLib.MAX_IMAGE_WIDTH_ALL_DEVICES/2, JSGFPLib.MAX_IMAGE_HEIGHT_ALL_DEVICES/2); 
		//mImageViewRegister.setImageBitmap(grayBitmap);
		//mImageViewVerify.setImageBitmap(grayBitmap); 

		mMaxTemplateSize = new int[1];

		//USB Permissions
		FDxSDKHelper.mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		context.registerReceiver(mUsbReceiver, filter);       	        
		sgfplib = new JSGFPLib((UsbManager) context.getSystemService(Context.USB_SERVICE));
		System.out.println("debug");
		//this.mCheckBoxSCEnabled.setChecked(true);

		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////

	}

	public static FDxSDKHelper getFDxSDKHelper(Context context){
		if(mFDxSDKHelper == null){
			mFDxSDKHelper = new FDxSDKHelper(context);
		}
		return mFDxSDKHelper;
	}

	//RILEY
	//This broadcast receiver is necessary to get user permissions to access the attached USB device
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG,"Enter mUsbReceiver.onReceive()");
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if(device != null){
							Log.d(TAG, "Vendor ID : " + device.getVendorId() + "\n");
							Log.d(TAG, "Product ID: " + device.getProductId() + "\n");
							//    						debugMessage("Vendor ID : " + device.getVendorId() + "\n");
							//    						debugMessage("Product ID: " + device.getProductId() + "\n");
						}
						else {
							Log.d(TAG, "mUsbReceiver.onReceive() Device is null");    						
						}    					
					} 
					else {
						Log.d(TAG, "mUsbReceiver.onReceive() permission denied for device " + device);
					}
				}
			}
		}
	};   
	//RILEY

	public static void setFingerprintBitmap(ImageView imageView){
		imageView.setImageBitmap(getFingerprintBitmap());
		imageView.setBackgroundColor(Color.argb(255, 0, 0, 0));
	}

	public static Bitmap getFingerprintBitmap(){

		SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
		FDxSDKHelper.sgfplib.GetDeviceInfo(deviceInfo);
		int mImageWidth = deviceInfo.imageWidth;
		int mImageHeight= deviceInfo.imageHeight;	
		byte[] buffer = new byte[mImageWidth*mImageHeight];
		ByteBuffer byteBuf = ByteBuffer.allocate(mImageWidth*mImageHeight);
		buffer = getImageBuffer();
		Bitmap b = Bitmap.createBitmap(mImageWidth,mImageHeight, Bitmap.Config.ARGB_8888);
		byteBuf.put(buffer);
		int[] intbuffer = new int[mImageWidth*mImageHeight];
		for (int i=0; i<intbuffer.length; ++i){
			intbuffer[i] = (int) buffer[i];
		}
		b.setPixels(intbuffer, 0, mImageWidth, 0, 0, mImageWidth, mImageHeight); 

		return b;
	}

	public static byte[] getImageBuffer(){

		FDxSDKHelper.sgfplib.OpenDevice(0);
		sgfplib.writeData((byte)5, (byte)1); //Enable Smart Capture
		SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
		FDxSDKHelper.sgfplib.GetDeviceInfo(deviceInfo);
		int mImageWidth = deviceInfo.imageWidth;
		int mImageHeight= deviceInfo.imageHeight;	
		byte[] buffer = new byte[mImageWidth*mImageHeight];
		ByteBuffer byteBuf = ByteBuffer.allocate(mImageWidth*mImageHeight);
		long result = FDxSDKHelper.sgfplib.GetImage(buffer); 
		//getTemplate();
		return buffer;
	}

	public static byte[] getTemplateAndSetImage(ImageView imageView){

		SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
		FDxSDKHelper.sgfplib.GetDeviceInfo(deviceInfo);
		int mImageWidth = deviceInfo.imageWidth;
		int mImageHeight= deviceInfo.imageHeight;
		byte[] mRegisterImage = new byte[mImageWidth*mImageHeight];
		sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
		byte[] mRegisterTemplate = new byte[mMaxTemplateSize[0]];


		ByteBuffer byteBuf = ByteBuffer.allocate(mImageWidth*mImageHeight);
		long result = sgfplib.GetImage(mRegisterImage);
		Bitmap b = Bitmap.createBitmap(mImageWidth,mImageHeight, Bitmap.Config.ARGB_8888);
		byteBuf.put(mRegisterImage);
		int[] intbuffer = new int[mImageWidth*mImageHeight];
		for (int i=0; i<intbuffer.length; ++i)
			intbuffer[i] = (int) mRegisterImage[i];
		b.setPixels(intbuffer, 0, mImageWidth, 0, 0, mImageWidth, mImageHeight); 
		Log.d(TAG, "Show Register image");
		imageView.setImageBitmap(b);
		imageView.setBackgroundColor(Color.argb(255, 0, 0, 0));
		result = sgfplib.SetTemplateFormat(SecuGen.FDxSDKPro.SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
		SGFingerInfo fpInfo = new SGFingerInfo();
		for (int i=0; i< mRegisterTemplate.length; ++i){
			mRegisterTemplate[i] = 0;
		}
		result = sgfplib.CreateTemplate(fpInfo, mRegisterImage, mRegisterTemplate);

		return mRegisterTemplate;
	}

	public static long setTemplateAndSetImage(byte[] template, ImageView imageView){

		SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
		FDxSDKHelper.sgfplib.GetDeviceInfo(deviceInfo);
		int mImageWidth = deviceInfo.imageWidth;
		int mImageHeight= deviceInfo.imageHeight;
		byte[] mRegisterImage = new byte[mImageWidth*mImageHeight];
		sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
		//template = new byte[mMaxTemplateSize[0]];

		//Création du template
		ByteBuffer byteBuf = ByteBuffer.allocate(mImageWidth*mImageHeight);
		long result = sgfplib.GetImage(mRegisterImage);
		sgfplib.SetTemplateFormat(SecuGen.FDxSDKPro.SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
		SGFingerInfo fpInfo = new SGFingerInfo();
		for (int i=0; i< template.length; ++i){
			template[i] = 0;
		}
		result = sgfplib.CreateTemplate(fpInfo, mRegisterImage, template);

		//Création du bitmap
		if(result == 0){
			template = FDxSDKHelper.cutByteArray(template, 400);
			Bitmap b = Bitmap.createBitmap(mImageWidth,mImageHeight, Bitmap.Config.ARGB_8888);
			byteBuf.put(mRegisterImage);
			int[] intbuffer = new int[mImageWidth*mImageHeight];
			for (int i=0; i<intbuffer.length; ++i)
				intbuffer[i] = (int) mRegisterImage[i];
			b.setPixels(intbuffer, 0, mImageWidth, 0, 0, mImageWidth, mImageHeight); 
			Log.d(TAG, "Show Register image");
			imageView.setImageBitmap(b);
			imageView.setBackgroundColor(Color.argb(255, 0, 0, 0));
			
			//Ajouté pour débuguer
//			byteBuf.clear();
//			b.recycle();
		}

		return result;
	}

	
	public static long setTemplate(byte[] template){

		SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
		FDxSDKHelper.sgfplib.GetDeviceInfo(deviceInfo);
		int mImageWidth = deviceInfo.imageWidth;
		int mImageHeight= deviceInfo.imageHeight;
		byte[] mRegisterImage = new byte[mImageWidth*mImageHeight];
		sgfplib.GetMaxTemplateSize(mMaxTemplateSize);

		//Création du template
		ByteBuffer byteBuf = ByteBuffer.allocate(mImageWidth*mImageHeight);
		long result = sgfplib.GetImage(mRegisterImage);
		sgfplib.SetTemplateFormat(SecuGen.FDxSDKPro.SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
		SGFingerInfo fpInfo = new SGFingerInfo();
		for (int i=0; i< template.length; ++i){
			template[i] = 0;
		}
		result = sgfplib.CreateTemplate(fpInfo, mRegisterImage, template);

		//Création du bitmap
		if(result == 0){
			template = FDxSDKHelper.cutByteArray(template, 400);
		}

		return result;
	}
	
	public static void setImageWithTemplate(ImageView imageView){

		SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
		FDxSDKHelper.sgfplib.GetDeviceInfo(deviceInfo);
		int mImageWidth = deviceInfo.imageWidth;
		int mImageHeight= deviceInfo.imageHeight;
		byte[] mRegisterImage = new byte[mImageWidth*mImageHeight];
		sgfplib.GetMaxTemplateSize(mMaxTemplateSize);

		ByteBuffer byteBuf = ByteBuffer.allocate(mImageWidth*mImageHeight);
		long result = sgfplib.GetImage(mRegisterImage);
		Bitmap b = Bitmap.createBitmap(mImageWidth,mImageHeight, Bitmap.Config.ARGB_8888);
		byteBuf.put(mRegisterImage);
		int[] intbuffer = new int[mImageWidth*mImageHeight];
		for (int i=0; i<intbuffer.length; ++i)
			intbuffer[i] = (int) mRegisterImage[i];
		b.setPixels(intbuffer, 0, mImageWidth, 0, 0, mImageWidth, mImageHeight); 
		Log.d(TAG, "Show Register image");
		imageView.setImageBitmap(b);
		imageView.setBackgroundColor(Color.argb(255, 0, 0, 0));
	}

	public static long getReturnCode(byte[] buffer){

		FDxSDKHelper.sgfplib.OpenDevice(0);
		sgfplib.writeData((byte)5, (byte)1); //Enable Smart Capture
		SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
		FDxSDKHelper.sgfplib.GetDeviceInfo(deviceInfo);
		int mImageWidth = deviceInfo.imageWidth;
		int mImageHeight= deviceInfo.imageHeight;	
		buffer = new byte[mImageWidth*mImageHeight];
		ByteBuffer byteBuf = ByteBuffer.allocate(mImageWidth*mImageHeight);
		long result = FDxSDKHelper.sgfplib.GetImage(buffer); 
		//getTemplate();
		return result;
	}

	public static int getTemplateSize(){
		sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
		return mMaxTemplateSize[0];
	}
	public static long getTemplate(byte[] mRegisterTemplate){

		byte[] mRegisterImage;
		FDxSDKHelper.sgfplib.OpenDevice(0);
		sgfplib.writeData((byte)5, (byte)1); //Enable Smart Capture
		SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
		FDxSDKHelper.sgfplib.GetDeviceInfo(deviceInfo);
		int mImageWidth = deviceInfo.imageWidth;
		int mImageHeight= deviceInfo.imageHeight;	
		mRegisterImage = new byte[mImageWidth*mImageHeight];
		ByteBuffer byteBuf = ByteBuffer.allocate(mImageWidth*mImageHeight);
		long result = FDxSDKHelper.sgfplib.GetImage(mRegisterImage); 

		long codeErreur = getReturnCode(mRegisterImage);
		if(codeErreur != SGFDxErrorCode.SGFDX_ERROR_NONE){
			return(codeErreur + 1000);
		}
		else{

			codeErreur = sgfplib.SetTemplateFormat(SecuGen.FDxSDKPro.SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
			SGFingerInfo fpInfo = new SGFingerInfo();
			for (int i=0; i< mRegisterTemplate.length; ++i){
				mRegisterTemplate[i] = 0;
			}
			codeErreur = sgfplib.CreateTemplate(fpInfo, mRegisterImage, mRegisterTemplate);

		}

		return codeErreur;
	}

	public static byte[] cutByteArray(byte[] tableau, int max){
		byte[] tableauReduit = new byte[max];
		for(int i = 0; (i < max && i < tableau.length); i++){
			tableauReduit[i] = tableau[i];
		}
		return tableauReduit;
	}

	public static boolean[] matchTemplates(byte[] template1, byte[] template2, boolean[] matched)
	{
		long sl = SGFDxSecurityLevel.SL_NORMAL;      // Set security level as NORMAL 
		sgfplib.MatchTemplate(template1, template2, sl, matched);
		return matched;
	}

	public static int[] getMatchingScore(byte[] template1, byte[] template2)
	{
		int[] score = new int[1];
		sgfplib.GetMatchingScore(template1, template2, score);
		return score;
	}
	
	public static void clearData(){
		grayBuffer = null;
		grayBitmap.recycle();
		mMaxTemplateSize = null;
	}

}
