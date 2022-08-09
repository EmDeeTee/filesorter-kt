import kotlin.system.exitProcess

class Options(args: Array<String>) {
    val Args = args
    var Verbose = false
    var Silent = false
    var Logging = false
    var ResetConfigFile = false
    fun Parse() {
        for (arg in Args) {
            if (arg == "-v") {
                Verbose = true
            }
            if (arg == "-s") {
                Silent = true
            }
            if (arg == "-l") {
                Logging = true
            }
            if (arg == "--reset-config") {
                ResetConfigFile = true
            }
            if (arg == "-h") {
                println("Usage [FirstArgument] [Flags...]")
                println("")
                println("[FirstArgument] | The directory to sort files in")
                println("Flags")
                println("-v | Verbose output")
                println("-s | Silent mode")
                println("-l | Enable logging")
                println("-h | help")
                println("--reset-config | Restores the config file to default")
                exitProcess(0)
            }
        }
    }
}