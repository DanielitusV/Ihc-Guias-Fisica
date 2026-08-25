using Microsoft.Win32;
using System;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using System.Runtime.InteropServices;
using System.Windows.Forms;

internal static class GuiasFisicaUninstaller
{
    private static readonly string[] ProductNames = { "Guías Física", "GuiasFisica" };
    private const uint ErrorNoMoreItems = 259;

    [DllImport("msi.dll", CharSet = CharSet.Unicode)]
    private static extern uint MsiEnumProducts(uint index, StringBuilder productCode);

    [DllImport("msi.dll", CharSet = CharSet.Unicode)]
    private static extern uint MsiGetProductInfo(
        string productCode,
        string property,
        StringBuilder value,
        ref int valueLength
    );

    [STAThread]
    private static void Main()
    {
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);

        string dataDirectory = Environment.GetEnvironmentVariable("GUIASFISICA_DATA_DIR");
        if (string.IsNullOrWhiteSpace(dataDirectory))
        {
            dataDirectory = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "GuiasFisica"
            );
        }

        bool testWithoutInstallation = string.Equals(
            Environment.GetEnvironmentVariable("GUIASFISICA_TEST_NO_INSTALL"),
            "1",
            StringComparison.Ordinal
        );
        string productCode = testWithoutInstallation ? null : FindProductCode();
        if (productCode == null)
        {
            HandleMissingInstallation(dataDirectory);
            return;
        }

        DialogResult dataChoice = MessageBox.Show(
            "¿También quieres borrar la base de datos?\n\n" +
            "Sí: elimina permanentemente ventas, stock e historial.\n" +
            "No: conserva los datos para reinstalar o actualizar.\n" +
            "Cancelar: no desinstala nada.",
            "Desinstalar Guías Física",
            MessageBoxButtons.YesNoCancel,
            MessageBoxIcon.Warning,
            MessageBoxDefaultButton.Button2
        );

        if (dataChoice == DialogResult.Cancel) return;

        try
        {
            Process process = Process.Start(new ProcessStartInfo(
                "msiexec.exe",
                "/x " + productCode
            ) { UseShellExecute = true });

            if (process == null) throw new InvalidOperationException("No se pudo iniciar Windows Installer.");
            process.WaitForExit();

            if (process.ExitCode == 1602) return;
            if (process.ExitCode != 0)
            {
                throw new InvalidOperationException(
                    "Windows Installer terminó con código " + process.ExitCode + "."
                );
            }

            if (dataChoice == DialogResult.Yes && Directory.Exists(dataDirectory))
            {
                Directory.Delete(dataDirectory, true);
            }

            MessageBox.Show(
                dataChoice == DialogResult.Yes
                    ? "Aplicación y base de datos eliminadas."
                    : "Aplicación eliminada. La base de datos fue conservada.",
                "Desinstalación completa",
                MessageBoxButtons.OK,
                MessageBoxIcon.Information
            );
        }
        catch (Exception exception)
        {
            MessageBox.Show(
                "No se pudo completar la desinstalación.\n\n" + exception.Message,
                "Error al desinstalar",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error
            );
        }
    }

    private static void HandleMissingInstallation(string dataDirectory)
    {
        if (!Directory.Exists(dataDirectory))
        {
            MessageBox.Show(
                "Guías Física no está instalada y no se encontraron datos locales.",
                "Desinstalar Guías Física",
                MessageBoxButtons.OK,
                MessageBoxIcon.Information
            );
            return;
        }

        DialogResult result = MessageBox.Show(
            "Guías Física no está instalada, pero existe una base de datos.\n\n" +
            "¿Quieres borrarla permanentemente?",
            "Borrar datos de Guías Física",
            MessageBoxButtons.YesNo,
            MessageBoxIcon.Warning,
            MessageBoxDefaultButton.Button2
        );

        if (result == DialogResult.Yes)
        {
            try
            {
                Directory.Delete(dataDirectory, true);
                MessageBox.Show("Base de datos eliminada.", "Datos eliminados");
            }
            catch (Exception exception)
            {
                MessageBox.Show(
                    "No se pudo borrar la base de datos.\n\n" + exception.Message,
                    "Error",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error
                );
            }
        }
    }

    private static string FindProductCode()
    {
        string code = FindInUninstallRegistry(RegistryHive.LocalMachine, RegistryView.Registry64);
        if (code != null) return code;
        code = FindInUninstallRegistry(RegistryHive.LocalMachine, RegistryView.Registry32);
        if (code != null) return code;
        code = FindInUninstallRegistry(RegistryHive.CurrentUser, RegistryView.Default);
        if (code != null) return code;
        return FindWithWindowsInstaller();
    }

    private static string FindInUninstallRegistry(RegistryHive hive, RegistryView view)
    {
        const string path = @"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall";
        using (RegistryKey baseKey = RegistryKey.OpenBaseKey(hive, view))
        using (RegistryKey uninstall = baseKey.OpenSubKey(path))
        {
            if (uninstall == null) return null;
            foreach (string name in uninstall.GetSubKeyNames())
            {
                using (RegistryKey product = uninstall.OpenSubKey(name))
                {
                    if (product == null) continue;
                    string displayName = product.GetValue("DisplayName") as string;
                    if (IsProductName(displayName)
                        && Regex.IsMatch(name, @"^\{[0-9A-Fa-f-]{36}\}$"))
                    {
                        return name;
                    }
                }
            }
        }
        return null;
    }

    private static string FindWithWindowsInstaller()
    {
        for (uint index = 0; ; index++)
        {
            StringBuilder productCode = new StringBuilder(39);
            uint result = MsiEnumProducts(index, productCode);
            if (result == ErrorNoMoreItems) return null;
            if (result != 0) continue;

            int length = 255;
            StringBuilder name = new StringBuilder(length + 1);
            if (MsiGetProductInfo(productCode.ToString(), "ProductName", name, ref length) == 0
                && IsProductName(name.ToString()))
            {
                return productCode.ToString();
            }
        }
    }

    private static bool IsProductName(string value)
    {
        foreach (string productName in ProductNames)
        {
            if (string.Equals(value, productName, StringComparison.OrdinalIgnoreCase)) return true;
        }
        return false;
    }
}
