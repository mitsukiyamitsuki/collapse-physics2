# Collapse Physics(崩落物理)for Minecraft 26.3 / Fabric

ブロックを壊したとき、地面につながっていない建材ブロックのかたまりが **まとめて落下** します。

- 木の根元の原木を切ると、幹が倒れずにそのまま落ちる
- 家の柱を全部壊すと、屋根が落ちてくる
- 橋の根元を壊すと、橋が落ちる

対象は原木・板材・レンガ・コンクリート・ガラス・羊毛・ハーフブロック・階段・フェンス・壁など(`CollapsePhysics.java` の `INCLUDE` / `EXCLUDE` で変更できます)。
土や石などの地形ブロックは「地面」として扱います。砂・砂利などは元々の落下ルールのままです。

## 必要なもの
- Minecraft Java 26.3
- Fabric Loader 0.19.5 以上
- Fabric API(26.3用)
- Java 25

## ビルド方法

### 方法A: 自分のPCでビルド
1. JDK 25(Temurinなど)と Gradle 9.6.0 を入れる
2. このフォルダで一度だけ `gradle wrapper --gradle-version 9.6.0` を実行
3. `./gradlew build`(Windowsは `gradlew.bat build`)
4. `build/libs/collapse-physics-1.0.0.jar` が完成 → `mods` フォルダへ

### 方法B: GitHubでビルド(PCに何も入れたくない場合)
1. このフォルダをそのままGitHubのリポジトリにアップロード
2. 「Actions」タブ → `build` → `Run workflow`
3. 完成したjarを「Artifacts」からダウンロード

## 調整できる所(`CollapsePhysics.java` の先頭)
- `MAX_NODES` … 一度に調べるブロック数の上限(大きいほど重い)
- `SCATTER` … 落ちるときの横ぶれ(0で真下)
- `INCLUDE` / `EXCLUDE` … 落ちる建材の種類
