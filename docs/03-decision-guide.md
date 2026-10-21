# 03. 判断の手引き: 迷いどころとトレードオフ

推奨アーキテクチャは「唯一の正解」ではなく、公式ガイド自身が「推奨であって必須ではない」と明言しています。
ここでは実際に設計するときに迷う点を、**何を得て何を払うか**の形で整理します。

## Q1. Domain 層(UseCase)は置くべきか

| 置く | 置かない |
|---|---|
| 複数画面で同じロジックを使う | ロジックが 1 画面にしか現れない |
| ViewModel が肥大化してきた(目安: 300 行) | ViewModel が十分小さい |
| ロジック単体でテストしたい | ViewModel のテストで十分カバーできる |

**払うもの**: クラス数が増える。「Repository を呼ぶだけ」の UseCase が量産されると、意味のない層になる。

**判断**: 最初は置かない。ViewModel に書き、2 回目に同じロジックが必要になった時点で UseCase に切り出す。
このリポジトリの 02(Todo)は置いていない、01(メモ)は示すために 1 つ置いている。どちらも妥当。

## Q2. UseCase の粒度は

- 1 UseCase = 1 つの業務上の操作(「メモを検索する」「ユーザーをフォローする」)。
- 「Repository のメソッドをそのまま呼ぶだけ」なら UseCase にしない(ViewModel から直接 Repository を呼ぶ)。
- UseCase が UseCase を呼ぶのは OK。UseCase が ViewModel や UI を知るのは NG。

## Q3. Hilt か手動 DI か

| Hilt | 手動 DI (AppContainer) |
|---|---|
| 画面が複数あり、依存関係が入り組んでいる(目安: 5 画面以上) | 画面が数個、依存が単純 |
| チームで作る(組み立てルールを強制したい) | 1 人で作る小さなアプリ、学習用 |
| ビルド時間の増加、アノテーションの学習コストを許容できる | ライブラリを増やしたくない |

**判断**: 公式は規模を問わず Hilt を推奨しており、手動 DI は学習用・極小アプリ向けの選択肢と考えてよい。
ただし **Hilt を入れる前に「依存をコンストラクタで受け取る」形にしておく**こと。
その形になっていれば、手動 DI から Hilt への移行は [Step 3 → Step 4](../02-refactor-legacy/docs/step4.md) のようにほぼ機械的。
逆にその形になっていなくても Hilt 自体は入れられるが、クラスが内部で `new` している依存には注入する余地がなく、何も良くならない。

## Q4. UI 状態は 1 つの data class か、複数の StateFlow か

公式のガイドは「互いに関連する状態は 1 つの UiState / 1 つのストリームにまとめる。無関係な状態や更新頻度が大きく違う状態は別ストリームにしてもよい」としている。
本書は **1 画面 1 UiState を基本形** として採用している。

- 利点: 画面の状態が「ある瞬間に 1 つの値」として確定する。状態同士の矛盾(isLoading = true なのに error もある、など)を設計で防げる。
- 欠点: 一部だけ変わっても全体が再送される(Compose は差分だけ再コンポーズするので通常は問題にならない)。
- 例外: TextField の入力テキストは UiState に入れず、スナップショット状態で別に持つ(Q7 の後の「入力テキスト」を参照)。

ViewModel の内部では複数の `MutableStateFlow` を持ち、`combine` で 1 つの UiState に合成するのが扱いやすい。
→ [NotesViewModel](../01-new-app/app/src/main/java/com/example/archnotes/ui/notes/NotesViewModel.kt)

## Q5. `isLoading` / `error` はフラグか、sealed class か

```kotlin
// A: フラグ方式
data class UiState(val items: List<Item>, val isLoading: Boolean, val error: String?)

// B: sealed interface 方式(各状態が UiState を実装していることが when の網羅チェックの条件)
sealed interface UiState {
    data object Loading : UiState
    data class Success(val items: List<Item>) : UiState
    data class Error(val message: String) : UiState
}
```

- A は「読込中だが前回の内容は表示しておく」「エラーだが一覧は出す」のような複合状態を表せる。画面の大半はこちらが自然。
- B は状態が排他的なとき(ログイン画面など)に when 式で網羅チェックが効いて安全。

**判断**: 一覧系はほぼ A。排他的な遷移が明確なときだけ B。

## Q6. ViewModel から UI への「イベント」はどう送るか

Snackbar 表示や画面遷移を `SharedFlow` / `Channel` で送る設計は、**公式に非推奨**になった。
理由は「配信と処理が保証されない」こと。replay のない SharedFlow は購読していない瞬間のイベントを失い、
Channel は UI が処理できない状態(バックグラウンドなど)のときに届いてしまい、UI 状態との整合性が取れなくなる。

**推奨**: イベントも状態にする。「メッセージを表示すべき」「保存が完了した」というフラグを UiState に持ち、UI が受け取ったら ViewModel に消してもらう。
UI 側は「受け取った」と伝えてから表示を始める(表示が終わるまで待つと、その間の回転で再表示されたり、同じメッセージの再発生を取りこぼしたりする)。
このやり方にもトレードオフはある: 表示中に回転すると Snackbar は消えて再表示されない(受け取り済みなので)。
公式ガイドの「ID 付きメッセージのリスト」方式なら、表示し終えるまで状態に残しつつ同じ内容の連続発生も区別できる。本書は簡潔さを優先して受け取り時に消す方式にしている。
→ `NotesViewModel.userMessageShown()`, `NoteEditorUiState.isSaved`, [NotesScreen](../01-new-app/app/src/main/java/com/example/archnotes/ui/notes/NotesScreen.kt) の `LaunchedEffect`

## Q7. StateFlow か LiveData か

新規なら **StateFlow**。
- Kotlin 標準で、Compose とも相性がよい(`collectAsStateWithLifecycle`)。
- `combine` / `map` / `flatMapLatest` などの変換が豊富。
- LiveData は View システムとの結合が強く、Domain/Data 層で使うべきではない。

既存アプリに LiveData があるなら無理に全部置き換える必要はない。新しく書く部分から StateFlow にしていけばよい。

**入力テキストだけは例外**: TextField に結びつけるテキストは StateFlow ではなく、ViewModel 内のスナップショット状態
(`var title by savedStateHandle.saveable { mutableStateOf("") }`)にする。
StateFlow → collect → 再コンポーズという非同期の経路を挟むと、速い入力や日本語の変換中に文字が欠ける・重複することがある。
→ [NoteEditorViewModel](../01-new-app/app/src/main/java/com/example/archnotes/ui/editor/NoteEditorViewModel.kt)

## Q8. Repository は `Result` を返すか、例外を投げるか

どちらでも成立する。要点は「UI まで例外を素通しにしない」こと。

- 例外方式(このリポジトリ): DataSource が「通信失敗は `IOException`」という契約を守り(Retrofit の `HttpException` などは DataSource 内で変換)、ViewModel がその型を catch して状態にする。コルーチンのキャンセル(`CancellationException`)を誤って握りつぶさないよう、`catch (e: Exception)` ではなく具体的な型で catch する。
- Result 方式: `Result<T>` や独自の sealed class を返す。呼び出し側が「失敗を扱わない」ことをコンパイル時に防げる。

## Q9. テストは Fake か Mock か

公式は **Fake を推奨**。
- Fake: インターフェースを本物らしく実装した軽量クラス(メモリ上のリストなど)。振る舞いを検証する。
- Mock: 呼び出しの記録・検証をする(Mockito など)。「このメソッドがこの引数で 1 回呼ばれた」を検証する。

Mock は実装の詳細(呼び出し順)に縛られ、リファクタリングでテストが壊れやすい。Fake は複数のテストで共有できる。
→ [FakeNotesRepository](../01-new-app/app/src/test/java/com/example/archnotes/testing/FakeNotesRepository.kt)

## Q10. モジュール分割はいつ

このリポジトリは単一モジュール(パッケージで層を分けている)。
モジュール分割(`:core:data`, `:feature:notes` など)が効くのは、

- ビルド時間が問題になってきた
- 複数チームが別々の機能を並行開発する
- 層をまたぐ不正な依存をコンパイルエラーにしたい

というとき。小さなアプリで最初からやると、Gradle 設定の手間が本体を上回る。
パッケージで層を分けておけば、後からモジュールに切り出すのは機械的。

## Q11. 既存アプリをどこから直すか

[02-refactor-legacy/docs/00-how-to-refactor.md](../02-refactor-legacy/docs/00-how-to-refactor.md) を参照。要約すると、

1. まず Data 層を切り出す(DB/API 呼び出しを Repository に)。最も効果が大きく、UI に影響しない。
2. 次に ViewModel + UiState。
3. その後で Room / Flow、DI、Compose。

## よくある誤解

- **「ViewModel = Activity のロジック置き場」**
  ViewModel は Activity の下請けではなく、画面の状態の持ち主。ViewModel が Activity の参照を持った時点で設計が壊れる。
- **「Repository は DAO のラッパー」**
  単なる委譲なら意味がない。Repository の価値は複数 DataSource の組み合わせ、SSOT の管理、モデルの変換にある。1 つの DataSource しかない段階でも、「将来そこに責任が集まる場所」としてインターフェースを切る価値はある。
- **「UseCase は必須」**
  公式も「省略可能」と言っている。「Repository を呼ぶだけ」の UseCase を量産しない。
- **「Compose にしたからアーキテクチャは不要」**
  Compose は UI 要素の作り方が変わるだけ。状態をどこに置くか、データをどう流すかという問題はそのまま残る。むしろ UDF を前提としている Compose では、アーキテクチャがないと状態管理が破綻しやすい。
- **「Hilt を入れればテストできるようになる」**
  テスト可能性を作るのは「依存をコンストラクタで受け取る + インターフェースに依存する」設計。Hilt はその組み立てを自動化するだけ。
- **「`viewModelScope.launch` の中で `withContext(Dispatchers.IO)`」**
  スレッド切り替えは Data 層の責任(main-safe)。ViewModel が Dispatchers を知っていたら、Repository の責務が漏れているサイン。

## 自分のアプリに当てはめるチェックリスト

- [ ] Activity / Fragment / Composable の中に、業務ルール(集計、バリデーション、条件分岐)がないか
- [ ] 画面回転で何かが失われないか(入力途中のテキスト、スクロール位置、選択状態)
- [ ] 同じデータを複数の場所で持っていないか(SSOT が決まっているか)
- [ ] 「DB を更新したあと手で再読み込みする」コードがないか(Flow にできないか)
- [ ] ViewModel を JVM 単体テストで new できるか(できないなら何が邪魔しているか)
- [ ] 依存を `new` している場所がクラスごとに散らばっていないか
