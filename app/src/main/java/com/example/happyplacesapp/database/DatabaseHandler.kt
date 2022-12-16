package com.example.happyplacesapp.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.happyplacesapp.models.HappyPlaceModel
import com.example.happyplacesapp.utils.Constants

class DatabaseHandler(context: Context) : SQLiteOpenHelper(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION ) {
    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_TABLE = ("CREATE TABLE" + Constants.TABLE_NAME + ","
                + Constants.KEY_ID + "INTEGER PRIMARY KEY,"
                + Constants.KEY_TITLE + "TEXT,"
                + Constants.KEY_DESCRIPTION + " TEXT,"
                + Constants.KEY_DATE + "TEXT,"
                + Constants.KEY_LOCATION + "TEXT,"
                + Constants.KEY_LATITUDE + "TEXT,"
                + Constants.KEY_LONGITUDE + "TEXT)")
        db?.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS ${Constants.TABLE_NAME}")
        onCreate(db)
    }

    fun addHappyPlace(model: HappyPlaceModel): Long{
        val db = this.writableDatabase

        val contentValues = ContentValues()
        contentValues.put(Constants.KEY_TITLE, model.title)
        contentValues.put(Constants.KEY_IMAGE, model.image)
        contentValues.put(Constants.KEY_DESCRIPTION, model.description)
        contentValues.put(Constants.KEY_DATE, model.date)
        contentValues.put(Constants.KEY_LOCATION, model.location)
        contentValues.put(Constants.KEY_LATITUDE, model.latitude)
        contentValues.put(Constants.KEY_LONGITUDE, model.longitude)

        val result = db.insert(Constants.TABLE_NAME, null, contentValues)
        db.close()
        return result
    }


}