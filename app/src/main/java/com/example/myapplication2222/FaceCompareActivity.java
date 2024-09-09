package com.example.myapplication2222;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FaceCompareActivity extends AppCompatActivity {

    private static final String SUBSCRIPTION_KEY = "ac630c029bc94df28abe01f0500a4bc8";
    private static final String ENDPOINT = "https://ocrcompare.cognitiveservices.azure.com/face/v1.0";

    private ImageView idFaceImageView;
    private ImageView userFaceImageView;
    private TextView comparisonResultTextView;

    private Bitmap idFaceBitmap;
    private Bitmap userFaceBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_compare);

        // UI 요소 초기화
        idFaceImageView = findViewById(R.id.idFaceImageView);
        userFaceImageView = findViewById(R.id.userFaceImageView);
        comparisonResultTextView = findViewById(R.id.comparisonResultTextView);
        Button compareButton = findViewById(R.id.compareButton);

        // Intent로부터 신분증 얼굴 Bitmap 받아오기
        Intent intent = getIntent();
        idFaceBitmap = intent.getParcelableExtra("idFaceBitmap");
        idFaceImageView.setImageBitmap(idFaceBitmap);

        // 사용자 얼굴 캡처 기능 추가 (여기서는 예제이므로 실제 카메라 로직이 필요합니다)
        // setUserFaceBitmap()을 통해 사용자 얼굴 Bitmap 설정
        // 예시로, 사용자 얼굴을 하드코딩하여 설정합니다. 실제로는 카메라 촬영 후 설정해야 합니다.
        userFaceBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.user_face_image); // 이 부분 수정 필요
        userFaceImageView.setImageBitmap(userFaceBitmap);

        // 버튼 클릭 시 얼굴 비교 시작
        compareButton.setOnClickListener(v -> compareFaces(userFaceBitmap, idFaceBitmap));
    }

    private void compareFaces(Bitmap userFace, Bitmap idFace) {
        new CompareFacesTask().execute(userFace, idFace);
    }

    private class CompareFacesTask extends AsyncTask<Bitmap, Void, String> {
        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            try {
                String faceId1 = detectFace(bitmaps[0]); // 사용자 얼굴
                String faceId2 = detectFace(bitmaps[1]); // 신분증 얼굴

                if (faceId1 != null && faceId2 != null) {
                    return verifyFaces(faceId1, faceId2); // 두 얼굴 비교
                }
            } catch (Exception e) {
                Log.e("FaceCompare", "Error comparing faces", e);
            }
            return null;
        }

        private String detectFace(Bitmap bitmap) throws IOException, JSONException {
            URL url = new URL(ENDPOINT + "/detect?returnFaceId=true");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Ocp-Apim-Subscription-Key", SUBSCRIPTION_KEY);
            connection.setDoOutput(true);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            OutputStream os = connection.getOutputStream();
            os.write(imageBytes);
            os.flush();
            os.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String response = readInputStream(connection.getInputStream());
                JSONArray jsonArray = new JSONArray(response);
                if (jsonArray.length() > 0) {
                    return jsonArray.getJSONObject(0).getString("faceId");
                }
            } else {
                Log.e("FaceCompare", "Face detection failed with code: " + connection.getResponseCode());
            }
            return null;
        }

        private String verifyFaces(String faceId1, String faceId2) throws IOException, JSONException {
            URL url = new URL(ENDPOINT + "/verify");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Ocp-Apim-Subscription-Key", SUBSCRIPTION_KEY);
            connection.setDoOutput(true);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("faceId1", faceId1);
            jsonObject.put("faceId2", faceId2);

            OutputStream os = connection.getOutputStream();
            os.write(jsonObject.toString().getBytes());
            os.flush();
            os.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String response = readInputStream(connection.getInputStream());
                return response;
            } else {
                Log.e("FaceCompare", "Face verification failed with code: " + connection.getResponseCode());
            }
            return null;
        }

        private String readInputStream(InputStream inputStream) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Log.d("FaceCompare", "Faces compared: " + result);
                // 비교 결과를 TextView에 표시
                comparisonResultTextView.setText("본인 인증 결과: " + result);
            } else {
                Log.e("FaceCompare", "Face comparison failed");
                comparisonResultTextView.setText("본인 인증 실패");
            }
        }
    }
}
