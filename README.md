# ViewLifecycle

Use plain android Views without Fragments, still having direct access to Lifecycle, ViewModel etc. 
Navigate with old well-known families of methods ViewGroup.addView and ViewGroup.removeView.

## Usage

Add ViewLifecycle dependency
```
implementation "com.eugene:viewlifecycle:1.1"
```
Use View extension property ```lifecycleOwner``` to access its lifecycle.

Use ViewGroup extension function ```trackNavigation``` to save navigation stack. Its direct children will be restored after configuration change.

See [todo app sample](https://github.com/es-andreev/android-architecture/tree/todo-mvvm-live-kotlin-fragmentless) based on this library.
