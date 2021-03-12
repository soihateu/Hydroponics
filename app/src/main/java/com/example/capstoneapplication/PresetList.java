package com.example.capstoneapplication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class PresetList extends MainMenu {

    DatabaseHelper db;
    ArrayList<String> listItem;
    ArrayAdapter adapter;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preset_items);

        // Database
        db = new DatabaseHelper(this);
        listItem = new ArrayList<>();
        listView = findViewById(R.id.item_list);

        viewData();

        // Interactions when list items are pressed
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String text = listView.getItemAtPosition(position).toString();
                Cursor cursor = db.viewData();
                cursor.moveToPosition(position);
                String light = Float.toString(cursor.getFloat(2));
                String humid = Float.toString(cursor.getFloat(3));
                String temp = Float.toString(cursor.getFloat(4));

                Toast.makeText(PresetList.this, "Light: "+ light + "% Humid: " + humid + "% Temp: " + temp + "°C", Toast.LENGTH_LONG).show();
            }
        });


        // Button to go back to main page
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PresetList.this, MainMenu.class);
                startActivity(intent);
            }
        });

        // Button to go to addPreset page
        Button addPreset = findViewById(R.id.addPreset);
        addPreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PresetList.this, EditPreset.class);
                startActivity(intent);
            }
        });
    }

    private void viewData() {
        Cursor cursor = db.viewData();

        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                listItem.add(cursor.getString(1));
            }
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItem);
        listView.setAdapter(adapter);
        cursor.close();
    }
}
