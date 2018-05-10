# Result<A, B>

This data type follow the `Either` functional pattern to return one of two possibilities with some renames to add meaning and extensions to work with futures. All the endpoints that deliver results in the SDK user some type `A` and an `Error` (different from `kotlin.Error`) as associated types. 

## Creating a `Result`

To create an instance of `Result` we can use:

 - For `Success`: `Result.success(value)` or `value.asSuccess()`
 - For `Failure`: `Result.failure(error)` or `error.asFailure()`
 
 Most of the time the type inference will deal with the type of the `Result`, however, in some circumstances may need to provide the types implicitly: `value.asSuccess<String, Error>()`
 
## Operations

 - `map`: Transforms success to another value. with `futureUser.map { it.id }` we end up with a `Result<String, Error>` potentially with the id of the user.
 - `flatMap`: Similar to `map` but, if successful we can provide a new result type. Useful to chain operations that may fail at different steps.
 - `swap`: converts `Result<A, B>` to `Result<B, A>`
 - `recover`: Can be used to, in case of getting an error, be able to convert to an instance of type `A`. i.e. `val text = result.recover { "oops" }`
 - `flatRecover`: Similar to `recover` but resulting in a new `Result` object instead of `A`
 - `flatten`: If we end with a type of the form `Result<A, A>` we can use this to get either of the values as `A`. Also, if we have a `Result<Result<A, B, B>` a function with the same name can be used to convert it to a basic `Result<A, B>`
 
## Operations with futures

As it is quite common to return a `Future<Result<A, B>>` we added a few functions that help working with both together:

- `mapResult`: short for `map { result -> result.map { ... } }` It tells it what to do when the result is ready and it is successful.
- `recoverResult`: Same as `recove` but inside a `Future`
- `recoverFutureResult`: Same as `recoverResult` but with the option to recover with a new `Future`
- `flatMapResult`: short for `map { result -> result.flatMap { ... } }`
- `flatMapFutureResult`: When the result is successful and ready we provide a new future that can produce a result with a different success value.
