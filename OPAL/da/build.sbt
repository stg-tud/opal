name := "Bytecode Disassembler"

version := "0.1.1-SNAPSHOT"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Disassembler") 

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.4"
