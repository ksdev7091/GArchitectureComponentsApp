# Android アプリ アーキテクチャ学習キット

Google が公式に推奨する Android アプリのアーキテクチャ
([Guide to app architecture](https://developer.android.com/topic/architecture)) を、
**考え方**と**手を動かせるコード**の両方から学ぶための教材です。

```
android-architecture-learning/
├── README.md                ← いまここ。全体の案内
├── docs/                    ← 「考え方」の解説(コードを読む前に)
│   ├── 01-architecture-mindset.md   なぜアーキテクチャが必要か、2 つの原則、SSOT、UDF
│   ├── 02-layers.md                 UI / Domain / Data 各層の責務とルール
│   ├── 03-decision-guide.md         迷いどころの判断基準とトレードオフ、よくある誤解
│   └── glossary.md                  用語集
├── 01-new-app/              ← パターン A: 新規アプリに最初から導入する(ArchNotes)
│   └── docs/                   演習問題と解答例
└── 02-refactor-legacy/      ← パターン B: 古い設計のアプリを段階的に直す(LegacyTodo)
    └── docs/                   各ステップの解説、演習問題、解答例
```

## 2 つのパターン

| | 01-new-app (ArchNotes) | 02-refactor-legacy (LegacyTodo) |
|---|---|---|
| 題材 | メモ帳(一覧・検索・編集・疑似サーバー同期) | Todo リスト(追加・完了・削除・件数集計) |
| 学ぶこと | 推奨アーキテクチャの「完成形」を一式そろえて読む | 「よくある古い Activity」を 5 段階で推奨形に直す |
| 技術 | Compose, Navigation, ViewModel, UseCase, Repository, Room, Retrofit + OkHttp, Hilt, 単体テスト(MockWebServer 含む) | Step 0: XML + SQLiteOpenHelper + Thread → Step 5: Compose + Room + Hilt + テスト |
| 構成 | 1 つのアプリモジュール、flavor で疑似リモート(demo)と本物の HTTP(prod)を切替 | 6 つのアプリモジュール(各ステップが独立して動く) |
| 演習 | [docs/exercises.md](01-new-app/docs/exercises.md): 機能追加で各層を通す 6 問 | [docs/exercises.md](02-refactor-legacy/docs/exercises.md): 各ステップ 2〜3 問、同じ機能を 4 段階で作って比較 |

どちらも Android Studio で開いてそのままビルド・実行できます(各フォルダがそれぞれ独立した Gradle プロジェクトです)。

## おすすめの学習順

1. **[docs/01-architecture-mindset.md](docs/01-architecture-mindset.md)** を読む(15 分)。
   「なぜ層を分けるのか」「状態はどこに置くべきか」という問いへの答えを先に持っておくと、コードの意図が読めるようになります。
2. **[02-refactor-legacy](02-refactor-legacy/README.md)** の Step 0 を読み、「どこが辛いか」を自分の言葉で説明してみる。
3. Step 1 → 5 を順に進める。各ステップの `docs/stepN.md` に「前のステップの課題 → 何を変えたか → なぜそれで解決するか」が書いてあります。
   隣り合うモジュールを Android Studio の **Compare Files**(2 ファイル選択 → 右クリック → Compare Files)で見比べるのが効果的です。
   各ステップの解説を読んだら、**[02 の演習](02-refactor-legacy/docs/exercises.md)** を解いてください。読むだけでは定着しません。
4. **[01-new-app](01-new-app/README.md)** を読む。リファクタリングで到達した形が、最初から設計されているとどうなるかを確認します。
   「検索語を入力してから画面が更新されるまで」のデータの流れを追う節があるので、実際にコードをたどってください。
   本物のネットワーク層(Retrofit)がどう組み込まれ、どうテストされているかもここで見ます。
5. **[01 の演習](01-new-app/docs/exercises.md)** で、完成形に機能を足すときに各層をどう通すかを手に入れる。
6. **[docs/02-layers.md](docs/02-layers.md)** と **[docs/03-decision-guide.md](docs/03-decision-guide.md)** で、
   自分のアプリに当てはめるときの判断基準を確認する。

## 動かし方

- Android Studio で `01-new-app` または `02-refactor-legacy` フォルダを **Open** する。
- `local.properties` の `sdk.dir` がこの PC の SDK パスを指しています。別の PC では Android Studio が自動生成します。
- 01 は Build Variants で `demoDebug`(疑似リモート、既定)か `prodDebug`(Retrofit で公開 API に接続)を選びます。
- 02 では実行構成のドロップダウンから `step0-legacy` 〜 `step5-compose` を選んで実行します。
- 単体テスト: 01 は `./gradlew testDemoDebugUnitTest`(24 件)、02 は `./gradlew testDebugUnitTest`(Step 4・5 に各 4 件)。
- 01 の `NoteDaoTest` は端末/エミュレータで動く Instrumented Test です(`./gradlew connectedDebugAndroidTest`)。
- Windows でフォルダのパスが極端に長い場所(260 文字近く)に置くと、`compileDebugJavaWithJavac` で jlink の起動に失敗することがあります。
  `C:\Users\<name>\AndroidStudioProjects\` のような通常の場所に置いてください。

## バージョン構成

AGP 8.13 / Kotlin 2.0.21 / Compose BOM 2024.12.01 / Room 2.7.0 / Hilt 2.52 / Retrofit 2.11 / OkHttp 4.12 / Gradle 8.13。
アイコンはシステム既定(`@android:drawable/sym_def_app_icon`)を使い、画像リソースを含めていません。

## 参考(公式)

- [アプリ アーキテクチャ ガイド](https://developer.android.com/topic/architecture)
- [UI レイヤ](https://developer.android.com/topic/architecture/ui-layer) / [ドメイン レイヤ](https://developer.android.com/topic/architecture/domain-layer) / [データ レイヤ](https://developer.android.com/topic/architecture/data-layer)
- [Android アプリのアーキテクチャに関する推奨事項](https://developer.android.com/topic/architecture/recommendations)
- 公式サンプル: [Now in Android](https://github.com/android/nowinandroid)(本格的)、[architecture-samples](https://github.com/android/architecture-samples)(Todo アプリ)
