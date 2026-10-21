# Step 4: Hilt と単体テスト

モジュール: `step4-hilt-test`(比較対象: `step3-room-flow`)

## 前のステップの課題

- `AppContainer` と `TodoViewModel.Factory` が手書き。画面が増えるたびに Factory を書く
- テストがない。Step 1〜3 で「テストできる形」にはなったが、まだ書いていない

## 何を変えたか

```
step4-hilt-test/src/
├── main/java/com/example/legacytodo/
│   ├── MainActivity.kt           @AndroidEntryPoint、by viewModels() に Factory 不要
│   ├── TodoApplication.kt        @HiltAndroidApp(AppContainer は削除)
│   ├── di/
│   │   ├── DataModule.kt         ★新設: TodoRepository → RoomTodoRepository の対応
│   │   └── DatabaseModule.kt     ★新設: Room の作り方
│   ├── data/RoomTodoRepository.kt    @Inject constructor, @Singleton
│   └── ui/TodoViewModel.kt           @HiltViewModel, @Inject constructor(Factory は削除)
└── test/java/com/example/legacytodo/
    ├── testing/MainDispatcherRule.kt   ★新設
    ├── testing/FakeTodoRepository.kt   ★新設
    └── ui/TodoViewModelTest.kt         ★新設(4 件)
```

## なぜこれで解決するのか

### Hilt は AppContainer の自動生成

Step 2〜3 の `AppContainer` を思い出してください:
```kotlin
val todoRepository: TodoRepository by lazy { RoomTodoRepository(database.todoDao()) }
```
Step 4 では同じことを 2 つのアノテーションで表現しています:
- `DataModule` の `@Binds fun bindTodoRepository(impl: RoomTodoRepository): TodoRepository` = 「TodoRepository が欲しいと言われたら RoomTodoRepository を渡す」
- `RoomTodoRepository @Inject constructor(todoDao: TodoDao)` = 「RoomTodoRepository は TodoDao があれば作れる」
- `DatabaseModule` の `@Provides` = 「TodoDao / TodoDatabase はこう作る」

Hilt はこれらをたどって、`TodoViewModel` を作るのに必要なものを全部組み立てます。
手書きの `Factory` が消えたのはそのためです。

**Hilt を入れたから何かができるようになったわけではありません。** Step 1〜3 で「依存をコンストラクタで受け取る」形にしていたから、Hilt が入れられた。順番が逆だと Hilt は導入できません。

### テスト: Step 0 では不可能だったこと

[TodoViewModelTest.kt](../step4-hilt-test/src/test/java/com/example/legacytodo/ui/TodoViewModelTest.kt) を読んでください。

```kotlin
val repository = FakeTodoRepository(listOf(todo(1, "a", isDone = true), todo(2, "b")))
val viewModel = TodoViewModel(repository)
assertEquals(1, viewModel.uiState.value.doneCount)
```

- エミュレータ不要。JVM で数ミリ秒。
- Step 0 の「完了数の集計」をテストするには、Activity を起動し、DB に行を入れ、TextView の文字列を読む必要がありました。
- これができるのは、ViewModel が (a) Android に依存せず、(b) Repository をインターフェースで受け取っているから。(b) は Step 1 で、(a) は Step 2 で手に入れたものです。

### Fake を使う理由

`FakeTodoRepository` は `MutableStateFlow<List<Todo>>` を持つだけの小さなクラスです。
Mockito のようなモックライブラリで「`addTodo` が 1 回呼ばれたこと」を検証する流儀もありますが、
Google は Fake を推奨しています。Fake は「振る舞い」を検証するので、実装の詳細(どのメソッドを何回呼ぶか)を変えてもテストが壊れません。

### テストの決まりごと

- `MainDispatcherRule`: `viewModelScope` は `Dispatchers.Main` を使うが JVM にはメインスレッドがないので、テスト用に差し替える。
- `backgroundScope.launch { viewModel.uiState.collect() }`: `stateIn(WhileSubscribed)` は購読者がいないと上流が動かない。テストでも購読しておく必要がある。

## 残っている課題

- UI が XML + `BaseAdapter`。リストの差分更新がなく、`notifyDataSetChanged()` で全部描き直している

次: [Step 5](step5.md) で UI を Compose に置き換えます。ViewModel とテストは一切触りません。
