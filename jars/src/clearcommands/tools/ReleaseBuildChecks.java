package clearcommands.tools;

public final class ReleaseBuildChecks {

    private static int checks;

    private ReleaseBuildChecks() {
    }

    public static void main(String[] args) throws Exception {
        checkNotes();
        checkRepoChangelog();
        checkStamping();
        checkFiles();
        System.out.println("Release build: " + checks + " checks passed");
    }

    private static void checkNotes() {
        String changelog = "header\n\n1.1.a - UNRELEASED\nAdded - New thing\n\n1.0.a\nFixed - Old thing\n";
        ReleaseBuild.Notes preview = ReleaseBuild.notes(changelog, null);
        require(preview.version().equals("1.1.a") && preview.unreleased(), "preview reads newest section");
        require(!preview.text().contains("Old thing"), "notes stop at the next section");
        fails(() -> ReleaseBuild.notes(changelog, "1.1.a"), "tag rejects UNRELEASED");
        fails(() -> ReleaseBuild.notes(changelog, "1.0.a"), "tag must match newest section");
        fails(() -> ReleaseBuild.notes(changelog, "v1.1.a"), "tag has no v prefix");
        String released = changelog.replace(" - UNRELEASED", "");
        require(ReleaseBuild.notes(released, "1.1.a").version().equals("1.1.a"), "tag accepts released section");
        fails(() -> ReleaseBuild.notes("1.0.a\nNothing here\n", null), "section needs a change line");
        fails(() -> ReleaseBuild.notes("1.0.a\nAdded - x\n1.0.a\nAdded - y\n", null), "duplicate versions");
    }

    private static void checkStamping() {
        String file = "{\"modVersion\":{\"major\":1,\"minor\":0,\"patch\":\"a\"},\"directDownloadURL\":\"x\"}";
        String stamped = ReleaseBuild.stampVersion(file, "2.3.c");
        require(stamped.contains("\"major\":2") && stamped.contains("\"minor\":3")
                && stamped.contains("\"patch\":\"c\""), "version file stamped");
        require(stamped.contains("/download/2.3.c/" + ReleaseBuild.FOLDER + "-2.3.c.zip"), "download url stamped");
        require(ReleaseBuild.stampInfo("{\"version\": \"0.1\"}", "2.3.c").contains("\"2.3.c\""), "mod_info stamped");
        fails(() -> ReleaseBuild.stampInfo("{}", "2.3.c"), "missing field rejected");
    }

    private static void checkRepoChangelog() throws Exception {
        String changelog = java.nio.file.Files.readString(java.nio.file.Path.of("Changelog"));
        require(ReleaseBuild.notes(changelog, null).version().matches(ReleaseBuild.VERSION), "repo changelog parses");
    }

    private static void checkFiles() throws Exception {
        require(ReleaseBuild.releaseFile("Changelog"), "changelog shipped");
        require(ReleaseBuild.releaseFile("ClearCommands.version"), "version file shipped");
        require(ReleaseBuild.releaseFile(ReleaseBuild.ICON), "luna icon shipped");
        require(ReleaseBuild.releaseFile(ReleaseBuild.COMMANDS), "command list shipped");
        require(ReleaseBuild.releaseFile("data/config/version/version_files.csv"), "data shipped");
        require(!ReleaseBuild.releaseFile("docs/ARCHITECTURE.md"), "docs skipped");
        require(!ReleaseBuild.releaseFile("CLAUDE.md"), "agent files skipped");
        require(!ReleaseBuild.releaseFile("lib/starfarer.api.jar"), "compile jars skipped");
        require(!ReleaseBuild.releaseFile("jars/ClearCommands.jar"), "ide jar skipped");
        require(!ReleaseBuild.releaseFile("ClearCommands.iml"), "ide files skipped");
        require(!ReleaseBuild.releaseFile("jars/src/" + ReleaseBuild.RUNTIME + "clearCommands_smartClear.java"),
                "source skipped");
        require(!ReleaseBuild.releaseFile("data/.idea/x.json"), "hidden dirs skipped");
        require(!ReleaseBuild.runtimeClass(ReleaseBuild.MOD + "/tools/ReleaseBuild.class"), "tools left out of jar");
        require(ReleaseBuild.runtimeClass(ReleaseBuild.RUNTIME + "ids/clearCommands_Ids.class"), "runtime classes kept");
        String csv = java.nio.file.Files.readString(java.nio.file.Path.of(ReleaseBuild.COMMANDS));
        require(ReleaseBuild.commandClasses(csv).size() == csv.strip().lines().count() - 1, "every command row read");
        for (String name : ReleaseBuild.commandClasses(csv)) {
            require(ReleaseBuild.runtimeClass(name), "command class in runtime package: " + name);
        }
        fails(() -> ReleaseBuild.commandClasses("command,class,tags\n"), "empty command list rejected");
    }

    private static void fails(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            checks++;
            return;
        }
        throw new AssertionError(message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
