package com.agomezmoron.saveImageGallery;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Arrays;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

/**
 * SaveImageGallery.java
 *
 * Extended Android implementation of the Base64ToGallery for iOS. Inspirated by StefanoMagrassi's code
 * https://github.com/Nexxa/cordova-base64-to-gallery
 *
 * @author Alejandro Gomez <agommor@gmail.com>
 */
public class SaveImageGallery extends CordovaPlugin {

   // Consts
   public static final String EMPTY_STR = "";

   public static final String JPG_FORMAT = "JPG";
   public static final String PNG_FORMAT = "PNG";

   // actions constants
   public static final String SAVE_BASE64_ACTION = "saveImageDataToLibrary";
   public static final String REMOVE_IMAGE_ACTION = "removeImageFromLibrary";

   @Override
   public boolean execute( String action, JSONArray args, CallbackContext callbackContext ) throws JSONException {
      if ( action.equals( SAVE_BASE64_ACTION ) ) {
         this.saveBase64Image( args, callbackContext );
      }
      else if ( action.equals( REMOVE_IMAGE_ACTION ) ) {
         this.removeImage( args, callbackContext );
      }
      else { // default case: SAVE_BASE64_ACTION
         this.saveBase64Image( args, callbackContext );
      }
      return true;
   }

   /**
    * It deletes an image from the given path.
    */
   private void removeImage( JSONArray args, CallbackContext callbackContext ) throws JSONException {
      String filename = args.optString( 0 );

      // isEmpty() requires API level 9
      if ( filename.equals( EMPTY_STR ) ) {
         callbackContext.error( "Missing filename string" );
      }

      File file = new File( filename );
      if ( file.exists() ) {
         try {
            file.delete();
         }
         catch ( Exception ex ) {
            callbackContext.error( ex.getMessage() );
         }
      }

      callbackContext.success( filename );

   }

   /**
    * It saves a Base64 String into an image.
    */
   private void saveBase64Image( JSONArray args, CallbackContext callbackContext ) throws JSONException {
      String content = args.optString( 0 );
      String filePrefix = args.optString( 1 );
      boolean mediaScannerEnabled = args.optBoolean( 2 );
      String format = args.optString( 3 );
      int quality = args.optInt( 4 );
      String folderPath = args.optString( 5 );

      List<String> allowedFormats = Arrays.asList( new String[] { JPG_FORMAT, PNG_FORMAT } );

      // isEmpty() requires API level 9
      if ( content.equals( EMPTY_STR ) ) {
         callbackContext.error( "Missing content string" );
         return;
      }

      // isEmpty() requires API level 9
      if ( format.equals( EMPTY_STR ) || !allowedFormats.contains( format.toUpperCase() ) ) {
         format = JPG_FORMAT;
      }

      // isEmpty() requires API level 9
      if ( folderPath.equals( EMPTY_STR ) ) {
         // folderPath = "/Pictures/";
      }

      if ( quality <= 0 ) {
         quality = 100;
      }

      // Save the image
      File imageFile = savePhoto( content.getBytes(), filePrefix, format, quality, folderPath );

      if ( imageFile == null ) {
         callbackContext.error( "Error while saving image" );
      }

      // Update image gallery
      if ( mediaScannerEnabled ) {
         scanPhoto( imageFile );
      }

      String path = imageFile.toString();
      if ( !path.startsWith( "file://" ) ) {
         path = "file://" + path;
      }
      callbackContext.success( path );
   }

   /**
    * Private method to save a {@link Bitmap} into the photo library/temp folder with a format, a prefix and with the given
    * quality.
    */
   private File savePhoto( byte[] bytes, String prefix, String format, int quality, String folderPath ) {
      try {
         File folder = createFolder( folderPath );
         if ( folder == null ) {
            return;
         }

         File nomediaFile = new File( folder, ".nomedia" );
         if ( !nomediaFile.exists() ) {
            nomediaFile.createNewFile();
         }

         return saveFile( bytes, folder, prefix + ".png" );
      }
      catch ( Exception ex ) {
         Log.e( "SaveImageToGallery", "An exception occured while saving image: " + ex.toString() );
         return null;
      }
   }

   private File createFolder( String folderPath ) {
      File folder;
      if ( EMPTY_STR.equals( folderPath ) ) {
         folder = new File( Environment.getExternalStorageDirectory() + "/Pictures/" );
      }
      else {
         folder = new File( folderPath );
      }
      if ( !folder.exists() ) {
         if ( !folder.mkdirs() ) {
            Log.e( "SaveImageToGallery", "Unable to create folder: " + folder.getAbsolutePath() );
            return null;
         }
      }
      return folder;
   }

   private File saveFile( byte[] bytes, File folder, String name ) throws Exception {
      File imageFile = new File( folder, name );

      FileOutputStream stream = null;
      try {
         stream = new FileOutputStream( imageFile );
         stream.write( bytes );
      }
      finally {
         if ( stream != null ) {
            stream.close();
         }
      }

      return imageFile;
   }

   /**
    * Invoke the system's media scanner to add your photo to the Media Provider's database, making it available in the Android
    * Gallery application and to other apps.
    */
   private void scanPhoto( File imageFile ) {
      Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE );
      Uri contentUri = Uri.fromFile( imageFile );

      mediaScanIntent.setData( contentUri );

      cordova.getActivity().sendBroadcast( mediaScanIntent );
   }
}