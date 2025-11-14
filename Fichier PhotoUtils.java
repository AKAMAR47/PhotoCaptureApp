package com.example.photocapture;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoUtils {

    public static Bitmap processPhotoForID(Bitmap originalBitmap, int targetWidth, int targetHeight) {
        // Créer un bitmap avec fond blanc
        Bitmap resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        
        // Remplir avec fond blanc
        canvas.drawColor(Color.WHITE);
        
        // Calculer le ratio pour redimensionner l'image originale
        float scale;
        float scaleX = (float) targetWidth / originalBitmap.getWidth();
        float scaleY = (float) targetHeight / originalBitmap.getHeight();
        scale = Math.min(scaleX, scaleY);
        
        // Calculer les nouvelles dimensions
        int newWidth = Math.round(originalBitmap.getWidth() * scale);
        int newHeight = Math.round(originalBitmap.getHeight() * scale);
        
        // Calculer la position pour centrer l'image
        int left = (targetWidth - newWidth) / 2;
        int top = (targetHeight - newHeight) / 2;
        
        // Redimensionner l'image originale
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
        
        // Dessiner l'image redimensionnée sur le fond blanc
        canvas.drawBitmap(scaledBitmap, left, top, new Paint());
        
        return resultBitmap;
    }

    public static String savePhotoWithWhiteBackground(Bitmap bitmap, String name, String directoryPath) {
        try {
            // Créer le nom de fichier avec timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = name.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timeStamp + ".jpg";
            
            File file = new File(directoryPath, fileName);
            
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
