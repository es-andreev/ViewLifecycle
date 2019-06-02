# ViewLifecycle

Use plain android Views without Fragments, still having direct access to Lifecycle, ViewModel etc. 

Navigation and back stack are managed by ```BackStackNavigator``` across configuration changes.

## Usage

Add ViewLifecycle dependency
```
implementation "com.eugene:viewlifecycle:1.2"
```
View extensions provided:
* ```lifecycleOwner``` to access its lifecycle
* ```viewModelProvider``` and ```viewModelProvider(ViewModelProvider.Factory?)``` for creating ViewModels
* ```arguments``` similarly to Fragments

See [todo app sample](https://github.com/es-andreev/android-architecture/tree/todo-mvvm-live-kotlin-fragmentless) based on this library.
