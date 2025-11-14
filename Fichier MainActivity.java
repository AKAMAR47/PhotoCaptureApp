package com.example.photocapture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQUEST_PICK_CSV_FILE = 103;

    private Button btnImportCSV, btnTakePhoto;
    private TextView tvCSVStatus, tvSelectedName;
    private ImageView ivPreview;

    private List<String> nameList;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        
        nameList = new ArrayList<>();
        
        // Demander les permissions
        requestPermissions();
    }

    private void initializeViews() {
        btnImportCSV = findViewById(R.id.btnImportCSV);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        tvCSVStatus = findViewById(R.id.tvCSVStatus);
        tvSelectedName = findViewById(R.id.tvSelectedName);
        ivPreview = findViewById(R.id.ivPreview);
    }

    private void setupClickListeners() {
        btnImportCSV.setOnClickListener(v -> importCSVFile());
        btnTakePhoto.setOnClickListener(v -> takePhoto());
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    REQUEST_STORAGE_PERMISSION);
        }
    }

    private void importCSVFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_PICK_CSV_FILE);
    }

    private void takePhoto() {
        if (nameList.isEmpty()) {
            Toast.makeText(this, "Veuillez d'abord importer un fichier CSV", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Erreur lors de la création du fichier", Toast.LENGTH_SHORT).show();
            }
            
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.photocapture.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_CSV_FILE) {
                if (data != null) {
                    Uri uri = data.getData();
                    readCSVFile(uri);
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                showNameSelectionDialog();
            }
        }
    }

    private void readCSVFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            CSVReader csvReader = new CSVReader();
            nameList = csvReader.readNamesFromCSV(inputStream);
            
            if (!nameList.isEmpty()) {
                tvCSVStatus.setText("Fichier CSV importé: " + nameList.size() + " noms");
                btnTakePhoto.setEnabled(true);
                Toast.makeText(this, "CSV importé avec succès!", Toast.LENGTH_SHORT).show();
            } else {
                tvCSVStatus.setText("Aucun nom trouvé dans le fichier CSV");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lors de la lecture du CSV", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showNameSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choisir un nom pour la photo");
        
        String[] namesArray = nameList.toArray(new String[0]);
        
        builder.setItems(namesArray, (dialog, which) -> {
            String selectedName = namesArray[which];
            tvSelectedName.setText("Nom sélectionné: " + selectedName);
            processAndSavePhoto(selectedName);
        });
        
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void processAndSavePhoto(String selectedName) {
        try {
            // Charger la photo capturée
            Bitmap originalBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            
            // Traiter la photo pour avoir un fond blanc et les dimensions 3.5x4.5cm
            Bitmap processedBitmap = PhotoUtils.processPhotoForID(originalBitmap, 350, 450);
            
            // Afficher l'aperçu
            ivPreview.setImageBitmap(processedBitmap);
            
            // Sauvegarder la photo
            String savedPath = PhotoUtils.savePhotoWithWhiteBackground(
                    processedBitmap, 
                    selectedName, 
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath()
            );
            
            Toast.makeText(this, "Photo sauvegardée: " + savedPath, Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lors du traitement de la photo", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission stockage refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
