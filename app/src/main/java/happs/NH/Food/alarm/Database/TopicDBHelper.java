package happs.NH.Food.alarm.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SH on 2016-04-02.
 */
public class TopicDBHelper extends SQLiteOpenHelper {

    public TopicDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String createSQL = "CREATE TABLE topics (" +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "name TEXT, " +
                                "mode INTEGER" +
                                ");";

        db.execSQL(createSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insert(String topic, int chmod) {
        SQLiteDatabase db = getWritableDatabase();
        if( !isExist(topic) )
            db.execSQL("INSERT INTO topics(name, mode) VALUES ('"+topic+"',"+chmod+")");
        db.close();
    }

    public void update(String _query) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(_query);
        db.close();
    }

    public void delete(String _query) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(_query);
        db.close();
    }

    public List<Topic> getTopicLists(){

        SQLiteDatabase db = getReadableDatabase();
        final String getQuery = "SELECT name, mode from topics";
        List<Topic> result = new ArrayList<>();

        Cursor cursor = db.rawQuery(getQuery, null);
        while(cursor.moveToNext()){
            result.add(new Topic(cursor.getString(0), cursor.getInt(1)));
        }
        cursor.close();

        return result;
    }

    private boolean isExist(String topic){
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * from topics WHERE name='"+topic+"'", null);
        int result = cursor.getCount();
        cursor.close();

        return (result > 0);
    }

}
