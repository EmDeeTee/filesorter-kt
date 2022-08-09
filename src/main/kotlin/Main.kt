import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import kotlin.io.path.name
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    var options = Options(args)
    options.Parse()
    val configFilePath = System.getProperty("user.home") + "/.config/filesorter.conf"
    var logString = ArrayList<String>()
    if (args.isEmpty()) {
        println("You need to provide at least one argument! Run this program with -h to show help.")
        return
    }
    if (!Files.exists(Paths.get(args[0]))) {
        println("First argument must be an existing directory!")
        return
    }
    if (!Files.exists(Paths.get(configFilePath))) {
        println("Can't find the config file in $configFilePath")
        println("Do you want to create it now? (y/n)")
        if (readln().lowercase() == "y") {
            Files.writeString(Paths.get(configFilePath), "-- Files are scanned from top to bottom, meanig that the ones on top will be moved first.\n" +
                    "folders = {\n" +
                    "\t{ \"mov\", \"mp3\", \"mp4\", \"media\" },\n" +
                    "\t{ \"txt\", \"rtf\", \"docx\", \"work\" },\n" +
                    "\t{ \"*\", \"rest\" }\n" +
                    "}\n" +
                    "\n" +
                    "-- This function will fun after the sorting is completed.\n" +
                    "function After() \n" +
                    "\tprint(\"After called!\")\n" +
                    "end")
            println("Now alter the config file in $configFilePath and re-run this program.")
            return
        }
        else {
            println("Please create the config file to continue.")
            return
        }
    }
    if (options.ResetConfigFile) {
        Files.writeString(Paths.get(configFilePath), "-- Files are scanned from top to bottom, meanig that the ones on top will be moved first.\n" +
                "folders = {\n" +
                "\t{ \"mov\", \"mp3\", \"mp4\", \"media\" },\n" +
                "\t{ \"txt\", \"rtf\", \"docx\", \"work\" },\n" +
                "\t{ \"*\", \"rest\" }\n" +
                "}\n" +
                "\n" +
                "-- This function will fun after the sorting is completed.\n" +
                "function After() \n" +
                "\tprint(\"After called!\")\n" +
                "end")
        println("The configuration file has been restored to default.")
        println("Alter the configuration file in ${configFilePath} and re-run this program.")
        exitProcess(0)
    }
    var files = ArrayList<String>()
    File(args[0]).listFiles().forEach {
        if (!it.isDirectory) {
            files.add(it.toString())
        }
    }

    if (!options.Silent) {
        if (files.size > 1)
            println("Found ${files.size} files in directory ${args[0]}")
        else
            println("Found ${files.size} file in directory ${args[0]}")
    }
    val configScript = Files.readString(Paths.get(configFilePath))

    val mgr = ScriptEngineManager()
    val lua = mgr.getEngineByName("luaj")
    try {
        lua.eval(configScript)
    }
    catch (e: ScriptException) {
        println("There are errors in the configuration file! Fix them or reset the configuration file by running this program with --reset-config.")
        println(e.message)
        exitProcess(1)
    }
    val luaFolders = lua.get("folders") as LuaTable
    var Sorters = ArrayList<Sorter>()
    var currentExtentions = ArrayList<String>()
    var currentFolder = String()
    var currentFiles = ArrayList<Path>()

    val red = "\u001b[31m"
    val yellow = "\u001b[33m"
    val green = "\u001b[32m"


    val reset = "\u001b[0m"
    for (i in 1..luaFolders.length()) {
        for (ii in 1..luaFolders[i].length()-1) {
            currentExtentions.add(luaFolders[i][ii].toString())
            if (options.Verbose) println("╠$yellow${luaFolders[i][ii]}$reset")
            if (ii == luaFolders[i].length()-1) {
                currentFolder = luaFolders[i][ii+1].toString()
                if (options.Verbose) println("╚$red${currentFolder}$reset")
            }

        }
        for (file in files) {
            for (ext in currentExtentions) {
                if (file.endsWith(ext)) {
                    currentFiles.add(Paths.get(file))
                }
            }
        }
        Sorters.add(Sorter(ArrayList<String>(currentExtentions), currentFolder, ArrayList<Path>(currentFiles)))
        currentExtentions.clear()
        currentFiles.clear()
    }

    var filesMoved = 0
    for (sorter in Sorters) {
        try {
            Files.createDirectory(Paths.get(args[0] + "/" + sorter.Folder))
        }
        catch (e: java.nio.file.FileAlreadyExistsException) {
            if (!options.Silent) println("Directory $red${sorter.Folder}$reset exists! Skipping...")
        }
        for (file in sorter.Files) {
            try {
                if (options.Verbose) {
                    println("Moving $file to ${args[0] + "/" + sorter.Folder + "/"}")
                }
                Files.move(file, Paths.get(args[0] + "/" + sorter.Folder + "/" + file.name))
                if (options.Logging) {
                    logString.add("Moving $file to ${args[0] + "/" + sorter.Folder + "/"}")
                }
                filesMoved++
            }
            catch (e: java.nio.file.FileAlreadyExistsException) {
                if (!options.Silent) println("File $yellow${file.name}$reset already exists in $red${sorter.Folder}$reset! Skipping...")
            }
        }
    }
    for (sorter in Sorters) {
        for (ext in sorter.Extentions) {
            if (ext == "*") {
                File(args[0]).listFiles()?.forEach {
                    try {
                        if (!it.isDirectory) {
                            if (options.Verbose) {
                                println("Moving $it to ${args[0] + "/" + sorter.Folder + "/"}")
                            }
                            Files.move(Paths.get(it.path), Paths.get(args[0] + "/" + sorter.Folder + "/" + it.name))
                            if (options.Logging) {
                                logString.add("Moving $it to ${args[0] + "/" + sorter.Folder + "/"}")
                            }
                            filesMoved++
                        }
                    }
                    catch (e: java.nio.file.FileAlreadyExistsException) {
                        if (!options.Silent) println("File $yellow${it.name}$reset already exists in $red${sorter.Folder}$reset! Skipping...")
                    }
                }
            }
        }
    }
    if (options.Logging) {
        val logFile = FileWriter(args[0] + "/log.txt")
        for (str in logString) {
            logFile.write(str + "\n")
        }
        logFile.close()
    }
    if (!options.Silent) {
        if (filesMoved > 0)
            println("Moved $green$filesMoved$reset files in total")
        else if (filesMoved == 0)
            println("${green}Moved no files$reset")
        else
            println("Moved $green$filesMoved$reset file in total")
    }
    if (lua.get("After") != null) {
        (lua.get("After") as LuaValue).call()
    }
}
