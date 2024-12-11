import java.lang.invoke.MethodHandle
import java.lang.foreign.*
import scala.util.control.NonFatal
import scala.util.Using

@main
def main(): Unit = {
    val getpidMachineCode: Array[Byte] = Array(
      0x48.toByte, 0xC7.toByte, 0xC0.toByte, 0x27.toByte, 0x00.toByte, 0x00.toByte, 0x00.toByte, // mov rax, 39
      0x0F.toByte, 0x05.toByte, // syscall
      0xC3.toByte // ret
    )

    Using(Arena.ofShared()) { arena =>
      try {
        val pageSize = 4096

        val codeSegmentSize = Math.max(getpidMachineCode.length, pageSize)

        val codeSegment = arena.allocate(codeSegmentSize, pageSize)
        codeSegment.copyFrom(MemorySegment.ofArray(getpidMachineCode))

        val linker = Linker.nativeLinker()

        val mprotectHandle = linker.downcallHandle(
          linker.defaultLookup().find("mprotect").get(),
          FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT)
        )

        val PROT_READ = 0x1
        val PROT_WRITE = 0x2
        val PROT_EXEC = 0x4

        val _ = mprotectHandle.invoke(codeSegment, codeSegmentSize, PROT_READ | PROT_EXEC).asInstanceOf[Int]

        val syscallHandle: MethodHandle = linker.downcallHandle(
          codeSegment,
          FunctionDescriptor.of(ValueLayout.JAVA_INT)
        )

        val result = syscallHandle.invoke().asInstanceOf[Int]
        println(s"Process ID: $result")
      } catch {
        case NonFatal(e) => e.printStackTrace()
      }
    }
}
