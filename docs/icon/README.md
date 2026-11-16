# 앱 아이콘

| 파일 | 용도 |
|---|---|
| `cashnote-icon.svg` | 원본. 108×108 좌표로 Android adaptive icon과 같다 |
| `cashnote-icon-512.png` | Play 스토어 앱 아이콘 (512×512, 32비트 PNG) |

앱 안의 아이콘은 `app/src/main/res/drawable/ic_launcher_foreground.xml`(전경), `ic_launcher_background.xml`(배경), `ic_launcher_monochrome.xml`(Android 13+ 테마 아이콘)이다. 원본 경로를 바꾸면 세 파일도 같이 바꾼다.

## 512 PNG 다시 만들기

런처에 실제로 보이는 건 가운데 72×72 영역(바깥 18은 마스크에 잘리는 여백)이라, 그 영역만 512로 그린다. 모서리 둥글림·그림자는 Play가 적용하므로 넣지 않는다.

1. 아래 HTML을 임시 파일로 저장하고 Edge headless로 캡처한다.

   ```html
   <!doctype html><html><body style="margin:0">
   <svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="18 18 72 72" style="display:block">
     <image href="file:///<저장소 경로>/docs/icon/cashnote-icon.svg" width="108" height="108"/>
   </svg></body></html>
   ```

   ```
   msedge --headless=new --disable-gpu --hide-scrollbars --allow-file-access-from-files ^
     --window-size=512,512 --screenshot=<출력.png> file:///<임시 html>
   ```

2. Edge 캡처는 24비트 RGB라, Play 규격에 맞게 32비트 RGBA로 다시 저장한다(예: Java `ImageIO`로 `TYPE_INT_ARGB`에 그려 저장).
3. PNG 헤더에서 크기 512×512, color type 6(RGBA)인지 확인한다.
