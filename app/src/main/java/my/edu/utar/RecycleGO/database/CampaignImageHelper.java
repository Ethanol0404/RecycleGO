package my.edu.utar.RecycleGO.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

public class CampaignImageHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CampaignImages.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "images";
    private static final String COL_ID = "campaign_id";
    private static final String COL_IMAGE = "image_data";

    public CampaignImageHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_IMAGE + " BLOB)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void saveImage(String campaignId, byte[] imageData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ID, campaignId);
        values.put(COL_IMAGE, imageData);
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public byte[] getImage(String campaignId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COL_IMAGE}, COL_ID + "=?", new String[]{campaignId}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            byte[] data = cursor.getBlob(0);
            cursor.close();
            return data;
        }
        if (cursor != null) cursor.close();
        return null;
    }
}
