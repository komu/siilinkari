package siilinkari.translator

import siilinkari.vm.OpCode

fun IR.translate(): OpCode = when (this) {
    IR.Not              -> OpCode.Not
    IR.Add              -> OpCode.Add
    IR.Subtract         -> OpCode.Subtract
    IR.Multiply         -> OpCode.Multiply
    IR.Divide           -> OpCode.Divide
    IR.Equal            -> OpCode.Equal
    IR.LessThan         -> OpCode.LessThan
    IR.LessThanOrEqual  -> OpCode.LessThanOrEqual
    IR.ConcatString     -> OpCode.ConcatString
    IR.Pop              -> OpCode.Pop
    IR.Call             -> OpCode.Call
    is IR.Push          -> OpCode.Push(value)
    is IR.LoadLocal     -> OpCode.LoadLocal(index, name)
    is IR.LoadGlobal    -> OpCode.LoadGlobal(index, name)
    is IR.LoadArgument  -> OpCode.LoadArgument(index, name)
    is IR.StoreLocal    -> OpCode.StoreLocal(index, name)
    is IR.StoreGlobal   -> OpCode.StoreGlobal(index, name)
    is IR.Jump          -> OpCode.Jump(label.address)
    is IR.JumpIfFalse   -> OpCode.JumpIfFalse(label.address)
}
