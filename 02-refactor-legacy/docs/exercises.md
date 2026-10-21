# 演習問題(02-refactor-legacy)

各ステップの解説を読んだあとに取り組んでください。解答例は [answers/](answers/) にあります。
**解答を見る前に、必ず自分で書いて動かしてください。** アーキテクチャの価値は「変更するときの手間」として体感するものなので、
読むだけでは身につきません。

演習は 2 本の軸で構成しています。

- **軸 A「完了済みを隠す」**: 同じ機能を Step 0 / 2 / 3 / 5 で実装し、変更箇所がどう変わるかを比べる。
- **軸 B「テストを書く」**: どの段階で何がテストできるようになるかを、実際に書いて確かめる。

各問題に ★ の数で難易度を付けています(★ = 15 分程度、★★★ = 1 時間以上)。

---

## Step 0 (legacy)

### 0-1 ★★ 完了済みを隠すトグル(軸 A)

`activity_main.xml` に `CheckBox`(「完了済みを隠す」)を追加し、ON のとき完了済みの Todo を一覧から隠してください。
「n 件中 m 件完了」の表示は**隠しても全件の数**を出すこと。

考えてほしいこと:
- フィルタはどこでかける? `loadTodos()` の SQL? `todos` を絞る? Adapter の中?
- 画面を回転したらトグルの状態はどうなる? 回転後も隠れたままにするには?
- 変更したファイルと行数をメモしておく(Step 2・3 で同じ機能を作って比較する)。

### 0-2 ★ 集計のバグをテストで捕まえられるか(軸 B)

`updateSummary()` の `todos.count { it.isDone }` を `todos.count { !it.isDone }` に書き換えて(わざとバグを入れて)、
**JVM の単体テストでこのバグを検出する方法を考えてください。**
書けない理由を、コードのどの行を指して説明できますか。元に戻すのを忘れずに。

### 0-3 ★ 回転中のクラッシュを再現する

`loadTodos()` の `Thread { ... }` の中、`val db = dbHelper.readableDatabase` の**直後**に `Thread.sleep(3000)` を入れ、
起動直後に画面を回転させてください。何が起きるか観察し、`MainActivity.kt` のコメント②と照らし合わせてください。
次に、同じ `sleep` を `Thread { ... }` の**先頭**(`readableDatabase` を取る前)に移して、挙動の違いを説明してください。

---

## Step 1 (repository)

Step 1〜3 のモジュールにはテスト用の依存が入っていません(テストは Step 4 で初めて足すため)。
この節と Step 2 の演習でテストを書くときは、先に `build.gradle.kts` に
`testImplementation(libs.junit)` と `testImplementation(libs.kotlinx.coroutines.test)` を足し、
Step 4 の `src/test/.../testing/MainDispatcherRule.kt` をコピーしてください。

### 1-1 ★★ FakeTodoRepository を書く(軸 B)

`TodoRepository` インターフェースを実装する `FakeTodoRepository` を `src/test` に書いてください
(まだテストは書かなくてよい。Fake が「書ける」ことを確かめるのが目的)。
書き終えたら Step 4 の `FakeTodoRepository.kt` と比べてください。

考えてほしいこと: Step 0 で同じものを書こうとしたら、何が邪魔になりますか。

### 1-2 ★ DB の知識が Activity から消えたか確認する

Step 1 の `MainActivity.kt` で `import android.database` / `ContentValues` / `SQLite` を検索してください。
残っていたら、それはどこにあるべきですか。

### 1-3 ★★ ioDispatcher の継ぎ目を使う

`SqliteTodoRepository` のコンストラクタにある `ioDispatcher` を使って、
Repository の `getTodos()` を JVM テストで呼ぶとどうなるか試してください(`StandardTestDispatcher` を渡す)。
なぜ動かないのか、Room 化(Step 3)でどう変わるのかを説明してください。

---

## Step 2 (viewmodel)

### 2-1 ★★ 完了済みを隠すトグル、ViewModel 版(軸 A)

0-1 と同じ機能を Step 2 で実装してください。条件:
- トグルの ON/OFF は `TodoUiState` に持つ(`hideDone: Boolean`)。
- 一覧に出す Todo は UiState の**計算プロパティ**で導く(`visibleTodos`)。元の `todos` は保持する。
- Activity は `viewModel.setHideDone(checked)` を呼ぶだけ。
- 回転しても隠れたまま。

0-1 で変更したファイル・行数と比べてください。

### 2-2 ★ 「読み込み中」の表示を確かめる

`SqliteTodoRepository.getTodos()` の先頭に `delay(2000)` を入れ、起動直後に「読み込み中…」が出ることを確認してください。
次に、`TodoUiState.isLoading` を `render()` で参照する行を消したら、何が起きますか。
「状態に入れたものは必ず画面に映す」のはなぜか、説明してください。

### 2-3 ★★ ViewModel を JVM だけで new する(軸 B)

1-1 の Fake を使って、`TodoViewModel(FakeTodoRepository(...))` を JVM テストで生成し、
`uiState.value.doneCount` が期待どおりかを検証するテストを**1 つだけ**書いてください。
`MainDispatcherRule` が必要になる理由を説明してください(Step 4 の `MainDispatcherRule.kt` を見てよい)。

---

## Step 3 (room+flow)

### 3-1 ★★ 完了済みを隠すトグル、Flow 版(軸 A)

2-1 と同じ機能を Step 3 で実装してください。条件:
- `hideDone` は `MutableStateFlow<Boolean>` として ViewModel に持つ。
- `uiState` は `combine(repository.observeTodos(), hideDone, errorMessageRes) { ... }` で導く。
- `loadTodos()` のような再読込は**書かない**。

2-1 との違いを説明してください。「状態を所有する」と「状態を導出する」の違いが見えましたか。

### 3-2 ★★ 件数を DB で数えるか、UiState で数えるか

`TodoDao` に `@Query("SELECT COUNT(*) FROM todos WHERE is_done = 1") fun observeDoneCount(): Flow<Int>` を追加し、
`doneCount` をそこから取る設計に変えてみてください。
変えたあとで、元の「`todos` から計算する」設計と比べて、どちらが良いか理由を付けて判断してください
(ヒント: データの二重管理、テーブルが 10 万行になったとき、テストの書きやすさ)。

### 3-3 ★ Flow が本当に「勝手に流れてくる」ことを確かめる

アプリを起動したまま、Android Studio の **App Inspection > Database Inspector** で `todos` テーブルに行を直接 INSERT してください。
画面はどうなりますか。Step 2 で同じことをすると、どうなりますか。

---

## Step 4 (hilt+test)

### 4-1 ★★ トグルのテストを書く(軸 A + B)

3-1 のトグルを Step 4 にも実装し、`TodoViewModelTest` に次の 2 つを追加してください。
- 「hideDone が true なら visibleTodos に完了済みが含まれない」
- 「hideDone を切り替えても totalCount / doneCount は変わらない」

### 4-2 ★★ Fake に遅延を入れて isLoading をテストする

`FakeTodoRepository` の `observeTodos()` を、`CompletableDeferred` を待ってから流す形に変え、
「待っている間は `isLoading == true`、流れたあとは `false`」を検証するテストを書いてください
(ヒント: 01-new-app の `FakeNotesRepository.refreshGate` が同じ仕組み)。

### 4-3 ★ Hilt の対応表を 1 行変えると何が起きるか

`DataModule` の `@Binds` 先を `RoomTodoRepository` から、`src/main` に移した `FakeTodoRepository` に変えてビルド・実行してください。
UI も ViewModel も触らずに「DB を使わないアプリ」になることを確認し、元に戻してください。
これが本番とテストで実装を切り替える仕組みの正体です(01-new-app の demo / prod flavor を参照)。

---

## Step 5 (compose)

### 5-1 ★★ 完了済みを隠すトグル、Compose 版(軸 A)

4-1 のトグルを Step 5 の `TodoScreen` に実装してください(Material3 の `Switch` または `Checkbox`)。
条件: 4-1 で変更した `TodoViewModel.kt` と `TodoViewModelTest.kt` を step5-compose にコピーし、それ以上は触らない。
UI 側は状態なし版 `TodoScreen` に `onHideDoneChange: (Boolean) -> Unit` を追加する。

0-1 / 2-1 / 3-1 / 5-1 の 4 回で、変更箇所がどう変わったかを表にまとめてください。

### 5-2 ★ Preview を 3 状態分つくる

状態なし版 `TodoScreen` に対して、「読み込み中」「空」「3 件(1 件完了)」の `@Preview` を追加してください。
ViewModel なしで見た目を確認できるのは、状態なし版に分けたおかげです。

### 5-3 ★★★ 入力中のテキストを ViewModel に移すべきか(発展)

Step 5 では入力中のテキストを `rememberSaveable` で UI 側に置いています。
これを ViewModel の `savedStateHandle.saveable { mutableStateOf("") }` に移した版を作り、
[docs/02-layers.md](../../docs/02-layers.md) の UI 層ルール 6 の基準に照らして、この画面ではどちらが適切か判断してください。
