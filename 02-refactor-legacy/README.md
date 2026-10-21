# 02-refactor-legacy: LegacyTodo — 古い設計のアプリを段階的に推奨アーキテクチャへ直す

Todo アプリ(追加・完了・削除・「n 件中 m 件完了」の集計)を題材に、
「よくある古い Activity」を 5 段階で推奨アーキテクチャに作り替えます。

各ステップは **独立したアプリモジュール** です。どのステップもビルド・実行でき、
隣り合うステップを Android Studio の Compare Files で見比べられます。

## ステップ一覧

| モジュール | テーマ | 解決する問題 | 主な変更 | 解説 |
|---|---|---|---|---|
| `step0-legacy` | 出発点 | (問題を観察する) | 1 つの Activity に UI・DB・スレッド・業務ロジックが同居 | [docs/step0.md](docs/step0.md) |
| `step1-repository` | Data 層を切り出す | DB アクセスとスレッド管理が Activity にある | `TodoRepository` インターフェース + SQLite 実装、`Thread` → コルーチン | [docs/step1.md](docs/step1.md) |
| `step2-viewmodel` | ViewModel と UiState | 状態と業務ロジックが Activity にある、回転で読み直す | `TodoViewModel` + `TodoUiState`、手動 DI(`AppContainer`)、UDF | [docs/step2.md](docs/step2.md) |
| `step3-room-flow` | Room + Flow | 書き込むたびに手動で再読込している | SQLiteOpenHelper → Room、`List` → `Flow`、`stateIn` | [docs/step3.md](docs/step3.md) |
| `step4-hilt-test` | Hilt と単体テスト | 組み立てが手書き、テストがない | `@HiltViewModel` など、`FakeTodoRepository`、`TodoViewModelTest` | [docs/step4.md](docs/step4.md) |
| `step5-compose` | Compose 化 | (UI の刷新。ViewModel は無変更) | XML + Adapter → `TodoScreen` | [docs/step5.md](docs/step5.md) |

最初に [docs/00-how-to-refactor.md](docs/00-how-to-refactor.md) で「どの順番で、なぜその順番で直すか」を読んでください。

## 進め方

1. `step0-legacy/.../MainActivity.kt` を読み、コメントの ①〜⑥ の問題を自分の言葉で説明してみる。
2. 各ステップの解説(`docs/stepN.md`)を読んでから、該当モジュールのコードを読む。
3. Compare Files で前ステップとの差分を確認する:
   Project ビューで 2 つのファイル(例: `step1-repository/.../MainActivity.kt` と `step2-viewmodel/.../MainActivity.kt`)を
   Ctrl クリックで選択 → 右クリック → **Compare Files**。
4. 実行構成で各ステップのアプリを起動して、動作が同じであることを確かめる。
   各ステップは applicationId が異なる別アプリとしてインストールされるので、データはどのステップ間でも共有されません(各アプリで入れ直してください)。

## 各ステップで変わるファイル / 変わらないファイル

「=」は前ステップのファイルを **コメントも含めてそのままコピー** したもので、Compare Files で差分ゼロになります
(解説コメントは、そのファイルを導入・変更したステップの時点のものが残ります)。
表にないファイルのうち `strings.xml` はアプリ名が各ステップで違い(Step 2 で `loading` も追加)、`AndroidManifest.xml` は Step 2 で Application クラスが、`themes.xml` は Step 5 でテーマが変わります。

```
                 step0  step1  step2  step3  step4  step5
MainActivity      ●      ●      ●      =      ●      ●     (= は前ステップとファイル単位で完全に同一)
Todo (model)      ●      移動   =      =      =      =
TodoDbHelper      ●      移動   =      削除
TodoRepository           ●      =      ●      =      =
Sqlite/RoomRepo          ●      =      ●      ●      =
TodoApplication                 ●      ●      ●      =
TodoViewModel                   ●      ●      ●      =
TodoAdapter                     ●      =      =      削除
Room (Entity/Dao/Db)                   ●      =      =
di/ (Hilt)                                     ●      =
TodoScreen (Compose)                                   ●
src/test                                       ●      =
```

「=」の多さが、層を分けたことの見返りです。
Step 3 で DB を Room に変えても MainActivity は無変更、Step 5 で UI を Compose に変えても ViewModel とテストは無変更です。

## 単体テスト

```bash
./gradlew :step4-hilt-test:testDebugUnitTest :step5-compose:testDebugUnitTest
```

Step 0〜3 にテストがないのは意図的です。Step 0 ではテストを書く「継ぎ目」が存在しません。
Step 1 で Repository をインターフェースにした時点で Fake が、Step 2 で ViewModel から Android 依存を外した時点で
ViewModel のテストが書ける形になり、Step 4 で実際に書く、という流れを体験してください。
