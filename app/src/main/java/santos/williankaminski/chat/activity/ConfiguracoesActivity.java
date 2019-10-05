package santos.williankaminski.chat.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import santos.williankaminski.chat.R;
import santos.williankaminski.chat.config.FirebaseConf;
import santos.williankaminski.chat.model.Permission;
import santos.williankaminski.chat.util.UserFirebase;

/**
 * @author Willian Kaminski dos santos
 * @since 05-10-2019
 * @version 0.0.1
 */
public class ConfiguracoesActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageButton imageButtonCamera;
    private ImageButton imageButtonGalery;
    private CircleImageView circleImageViewFotoPerfil;

    private StorageReference storageReference;
    private String userId;

    private static final int CAMERA = 100;
    private static final int GALLERY = 200;

    private String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes);

        initComponents();
        confgToolbar();
        firedaseUseData();

        // Validar as permissões
        Permission.validatePermission(permissions, this, 1);

        buttonEvents();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK ){

            Bitmap image = null;

            try {

                switch (requestCode){
                    case CAMERA:
                        image = (Bitmap) data.getExtras().get("data");
                        break;
                    case GALLERY:
                        Uri localImage = data.getData();
                        image = MediaStore.Images.Media.getBitmap(getContentResolver(), localImage);
                        break;
                }

                if(image != null){

                    circleImageViewFotoPerfil.setImageBitmap(image);

                    // Recuperar dados da imagem para o Firebase
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
                    byte[] dataImage = byteArrayOutputStream.toByteArray();

                    // Salvar imagem no Firebase
                    StorageReference imageRef = storageReference
                            .child("imagens")
                            .child("perfil")
                            .child(userId)
                            .child("perfil.jpeg");

                    UploadTask uploadTask = imageRef.putBytes(dataImage);

                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(
                                    getApplicationContext(),
                                    getResources().getString(R.string.upload_image_error),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(
                                    getApplicationContext(),
                                    getResources().getString(R.string.upload_image_sucess),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            }catch (Exception e){

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for(int permissionResult: grantResults){
            if(permissionResult == PackageManager.PERMISSION_DENIED){
                alertValidate();
            }
        }
    }

    public void alertValidate(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.permission_alert_title));
        builder.setMessage(getResources().getString(R.string.permission_alert_message));
        builder.setCancelable(false);
        builder.setPositiveButton(getResources().getString(R.string.permission_alert_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });

        builder.create().show();
    }

    public void initComponents(){
        toolbar = findViewById(R.id.toolbarPrincipal);
        imageButtonGalery = findViewById(R.id.imageButtonGalery);
        imageButtonCamera = findViewById(R.id.imageButtonCamera);
        circleImageViewFotoPerfil = findViewById(R.id.circleImageViewFotoPerfil);
    }

    public void confgToolbar(){
        toolbar.setTitle(getResources().getString(R.string.toolbar_confg));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void buttonEvents(){

        imageButtonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if(intent.resolveActivity(getPackageManager()) !=  null){
                    startActivityForResult(intent, CAMERA);
                }


            }
        });

        imageButtonGalery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                if(intent.resolveActivity(getPackageManager()) !=  null){
                    startActivityForResult(intent, GALLERY);
                }


            }
        });
    }

    public void firedaseUseData(){
        storageReference = FirebaseConf.getFirebaseStorage();
        userId = UserFirebase.getUserId();
    }
}
