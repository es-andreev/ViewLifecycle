# ViewLifecycle

Use plain android Views without Fragments, still having direct access to Lifecycle, ViewModel etc. 

Navigation and back stack are managed by ```BackStackNavigator``` across configuration changes.

## Usage

Add ViewLifecycle dependency
```
implementation "ru.viewlifecycle:viewlifecycle:1.12-alpha"
```
View extensions provided:
* ```viewModels()``` for creating ViewModels
* ```lifecycleOwner``` to access its lifecycle
* ```arguments``` similarly to Fragments

See [todo app sample](https://github.com/es-andreev/android-architecture/tree/todo-mvvm-live-kotlin-fragmentless) based on this library.
