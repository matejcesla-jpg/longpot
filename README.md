# LongPot for Paper 1.21.11

## Build on GitHub (no local installation)
1. Create a new GitHub repository.
2. Upload **the contents of this LongPot folder** to the repository root, including `.github/workflows/build.yml` (GitHub web upload may not preserve hidden folders reliably; check that the workflow file exists).
3. Open **Actions** > **Build LongPot** > **Run workflow**.
4. Open the completed run and download the **LongPot-Plugin** artifact.
5. Unzip the artifact, then put `LongPot-1.0.0.jar` in your Paper server's `plugins` folder and restart.

Requires Java 21. If compilation fails, inspect the error in the **Build plugin** step.
