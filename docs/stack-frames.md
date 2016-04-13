# Stack frame layout

This document describes the stack frame layout in the final code, as well as the function calling
convention that goes with the layout.

## Basic layout

The stack frame consists of four parts:

  1. arguments to the current function
  2. return address
  3. local variables
  4. temporary variables

Only the return address is mandatory, other parts of the frame may be empty. (The distinction between local
variables and temporaries is a more of an artifact of the code generation than anything fundamental.)

So a concrete stack frame could be something like the following:

| fp   | Address | Offset | Purpose        | Expression                 |
| ---: | ------- | ------ | -------------- | -------------------------- |
|   => | 1234    | 0      | argument 0     | fp + 0                     |
|      | 1235    | 1      | argument 1     | fp + 1                     |
|      | 1236    | 2      | argument 2     | fp + 2                     |
|      | 1237    | 3      | return address | fp + args                  |
|      | 1238    | 4      | local 0        | fp + args + 1 + 0          |
|      | 1239    | 5      | local 1        | fp + args + 1 + 1          |
|      | 1240    | 6      | local 2        | fp + args + 1 + 2          |
|      | 1241    | 7      | temp 0         | fp + args + 1 + locals + 0 |
|      | 1242    | 8      | temp 1         | fp + args + 1 + locals + 1 |
|      | 1243    | 9      | temp 2         | fp + args + 1 + locals + 2 |
|      | 1244    | 10     | temp 3         | fp + args + 1 + locals + 3 |
|      | 1245    | 11     | temp 4         | fp + args + 1 + locals + 4 |
|      | 1246    | 12     | temp 5         | fp + args + 1 + locals + 5 |
|      | 1247    | 13     | temp 6         | fp + args + 1 + locals + 6 |
|      | 1248    | 14     | temp 6         | fp + args + 1 + locals + 7 |

A frame pointer (fp) points to the beginning of address, allowing us to translate stack-relative offsets into
real addresses.

Note that even though functions have a static guarantees on stack usage (we can calculate the frame-size at compile
time), nothing in our stack frame layout actually requires bounded size: we could allocate more temporaries as we go
because there's always free space after the current frame. The area of temporaries could therefore be used as an
unbounded stack.

## Function calls

When evaluating function calls, we will first evaluate all arguments and the function to be called into
successive temporaries in the end of our frame (that is, even if we plan to use more temporaries in the future,
at the moment all the following temporaries are unused).

| fp   | Address | Offset | Purpose        | Value     |
| ---: | ------- | ------ | -------------- | --------- |
|   => | 1234    | 0      | argument 0     |           |
|      | 1235    | 1      | argument 1     |           |
|      | 1236    | 2      | argument 2     |           |
|      | 1237    | 3      | return address |           |
|      | 1238    | 4      | local 0        |           |
|      | 1239    | 5      | local 1        |           |
|      | 1240    | 6      | local 2        |           |
|      | 1241    | 7      | temp 0         |           |
|      | 1242    | 8      | temp 1         | arg 0     |
|      | 1243    | 9      | temp 2         | arg 1     |
|      | 1244    | 10     | temp 3         | func      |
|      | 1245    | 11     | temp 4         | ---       |


After this we'll execute `call frame[10], 2` which tells us that the function is at `frame[10]` and it has
two arguments in preceding locations. The call replaces `func` at `frame[10]` with the address of the
instruction following call, updates the frame pointer (fp) to point at `1242` (the address of the first argument)
and then jumps to the address of `func`.

| fp   | Address | Offset | Purpose          |
| ---: | ------- | ------ | ---------------- |
|      | 1234    | -8     | (argument 0)     |
|      | 1235    | -7     | (argument 1)     |
|      | 1236    | -6     | (argument 2)     |
|      | 1237    | -5     | (return address) |
|      | 1238    | -4     | (local 0)        |
|      | 1239    | -3     | (local 1)        |
|      | 1240    | -2     | (local 2)        |
|      | 1241    | -1     | (temp 0)         |
|   => | 1242    | 0      | argument 0       |
|      | 1243    | 1      | argument 1       |
|      | 1244    | 2      | return address   |
|      | 1245    | 3      | local 0          |
|      | 1246    | 4      | local 1          |
|      | 1247    | 5      | temp 0           |
|      | 1248    | 6      | temp 1           |
|      | 1249    | 7      | temp 2           |

Note how `local 0` is stored at address `1235` which is also part of the callers frame. However, since the
caller is not using it during the call, we may safely use the location until we return.

## Returning from function calls

Getting rid of our stack frame is one thing, but how do we restore the original frame when returning from a
function?

First the called function replaces the head of frame with the value it wants to return. (e.g. `frame[0] = frame[5]`)
and then executes `jump frame[2]` go back to the instruction after the call. Now it's up to the caller to restore
the frame pointer. It therefore has an instruction following the original `call frame[10], 2` to set fp back to
the original value.
