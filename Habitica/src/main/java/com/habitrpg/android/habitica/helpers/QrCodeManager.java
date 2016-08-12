package com.habitrpg.android.habitica.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Environment;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.habitrpg.android.habitica.R;
import com.habitrpg.android.habitica.ui.AvatarView;
import com.magicmicky.habitrpgwrapper.lib.models.HabitRPGUser;
import com.raizlabs.android.dbflow.runtime.transaction.BaseTransaction;
import com.raizlabs.android.dbflow.runtime.transaction.TransactionListener;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

import net.glxn.qrgen.android.QRCode;
import net.glxn.qrgen.core.image.ImageType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by keithholliday on 8/12/16.
 */
public class QrCodeManager {

    //@TODO: Allow users to set other content
    private String content;
    private Context context;

    private ImageView qrCodeImageView;
    private Button qrCodeDownloadButton;
    private AvatarView avatarView;
    private FrameLayout qrCodeWrapper;

    private String albumnName;
    private String fileName;
    private String saveMessage;

    private TransactionListener<HabitRPGUser> userTransactionListener = new TransactionListener<HabitRPGUser>() {
        @Override
        public void onResultReceived(HabitRPGUser habitRPGUser) {
            QrCodeManager.this.avatarView.setUser(habitRPGUser);
        }

        @Override
        public boolean onReady(BaseTransaction<HabitRPGUser> baseTransaction) {
            return true;
        }

        @Override
        public boolean hasResult(BaseTransaction<HabitRPGUser> baseTransaction, HabitRPGUser habitRPGUser) {
            return true;
        }
    };

    public QrCodeManager(Context context, LinearLayout qrLayout) {
        this.context = context;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        String userId = prefs.getString(this.context.getString(R.string.SP_userID), "");

        this.albumnName = this.context.getString(R.string.qr_album_name);
        this.fileName = this.context.getString(R.string.qr_file_name);
        this.saveMessage = this.context.getString(R.string.qr_save_message);

        this.qrCodeImageView = (ImageView) qrLayout.findViewById(R.id.QRImageView);
        this.qrCodeDownloadButton = (Button) qrLayout.findViewById(R.id.QRDownloadButton);
        this.avatarView = (AvatarView) qrLayout.findViewById(R.id.avatarView);
        this.qrCodeWrapper = (FrameLayout) qrLayout.findViewById(R.id.qrCodeWrapper);

        this.content = userId;

        //@TODO: Move to user helper/model
        new Select()
                .from(HabitRPGUser.class)
                .where(Condition.column("id")
                .eq(userId))
                .async()
                .querySingle(userTransactionListener);

        this.displayQrCode();
        this.setDownloadQr();
    }

    public void displayQrCode() {
        if (qrCodeImageView == null) {
            return;
        }

        Bitmap myBitmap = QRCode.from(this.content).bitmap();
        qrCodeImageView.setImageBitmap(myBitmap);
    }

    public void setDownloadQr() {
        if (qrCodeDownloadButton == null) {
            return;
        }

        qrCodeDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File dir = getAlbumStorageDir(context, albumnName);
                dir.mkdirs();

                File pathToQRCode = new File(dir, fileName);
                try {
                    pathToQRCode.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    FileOutputStream outputStream = new FileOutputStream(pathToQRCode);

                    qrCodeWrapper.setDrawingCacheEnabled(true);
                    Bitmap b = qrCodeWrapper.getDrawingCache();
                    b.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);

                    outputStream.close();

                    Toast.makeText(context, saveMessage + pathToQRCode.getPath(),
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private File getAlbumStorageDir(Context context, String albumName) {
        // Get the directory for the app's private pictures directory.
        File file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), albumName);
        file.mkdirs();
        return file;
    }
}
