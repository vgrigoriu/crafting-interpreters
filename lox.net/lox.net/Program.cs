using System;
using System.IO;
using System.Threading.Tasks;

namespace lox.net
{
    class Program
    {
        private static bool hadError = false;

        static async Task Main(string[] args)
        {
            if (args.Length > 1)
            {
                await Console.Out.WriteLineAsync("Usage: dotnet run [script]");
                Environment.Exit(64);
            }
            else if (args.Length == 1)
            {
                await RunFileAsync(args[0]);
            }
            else
            {
                await RunPromptAsync();
            }
        }

        private static async Task RunFileAsync(string path)
        {
            // This should autodetect utf8 and utf32 based on byte-order marks.
            var fileContent = await File.ReadAllTextAsync(path);
            await RunAsync(fileContent);

            if (hadError) {
                Environment.Exit(65);
            }
        }

        private static async Task RunPromptAsync()
        {
            var inputStream = Console.In;
            while (true)
            {
                await Console.Out.WriteAsync("> ");
                var line = await inputStream.ReadLineAsync();
                if (line is null) {
                    // This means we reached end of file.
                    await Console.Out.WriteLineAsync();
                    await Console.Out.WriteLineAsync("Bye!");
                    return;
                }
                await RunAsync(line);
                hadError = false;
            }
        }

        private static async Task RunAsync(string source)
        {
            var scanner = new Scanner(source);
            var tokens = scanner.ScanTokensAsync();

            // For now, just print the tokens.
            await foreach (var token in tokens)
            {
                Console.WriteLine(token);
            }
        }

        public static async Task ErrorAsync(int line, string message)
        {
            await ReportAsync(line, "", message);
        }

        private static async Task ReportAsync(int line, string where, string message)
        {
            await Console.Error.WriteLineAsync($"[line {line}] Error{where}: {message}");
            hadError = true;
        }
    }
}
