# ViewLifecycle

Use plain android Views without Fragments, still having direct access to Lifecycle, ViewModel etc. 
Navigate with old well-known families of methods ViewGroup.addView and ViewGroup.removeView.

## Usage

Add ViewLifecycle dependency
```
implementation "ru.viewlifecycle:viewlifecycle:1.3-alpha"
```
Use View extension function ```viewModels``` to create ViewModels.

Use View extension property ```lifecycleOwner``` to access its lifecycle.

Use ```BackStackNavigator(viewGroup)``` for back stack navigation.

See [todo app sample](https://github.com/es-andreev/android-architecture/tree/todo-mvvm-live-kotlin-fragmentless) based on this library.
