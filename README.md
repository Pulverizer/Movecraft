# Movecraft-for-Sponge

Bringing Movecraft to Sponge, one bug at a time!  
This project began when the Bukkit DMCA resulted in the death of Cauldron.  
We aim to fix that itch for buildable vichles in your modded worlds, the support of SpongeVanilla is a side effect of this process.  
Some additional features are being added as a part of this process.  



### Discord
https://discord.gg/5Wj8yxy  



### Setting up your Workspace

1. Install and set up IntelliJ IDEA
2. Install the IDE plugin "Minecraft Development"
3. Clone and Setup the SpongeVanilla repository following their [IntelliJ instructions](https://github.com/SpongePowered/SpongeVanilla#cloning)
3. Clone this repository
4. Open the SpongeVanilla repository in the IDE
5. Add this repository as a project module using File > Project Structure > Project Settings > Modules > Add
5. Add the module `Movecraft.main` as a module dependency of the `SpongeVanilla.main` module 
5. Wait for the repository to be indexed
5. Run the gradle task `setupDecompWorkspace`
6. Refresh the gradle dependencies
7. Theoretically, you are now ready to code or compile



### Compiling the Repository

1. Run the `build` gradle task
2. Find the output jar in `\build\libs`



### The Development Process

We are currently attempting to implement Test Driven Development using Junit and Mockito.  

New GitHub issues are opened as problems are located within the codebase during development.  
Development is guided by open GitHub issues.  
Bugs, Missing Features, and Code Health are prioritised over new features.  




### Latest JavaDocs
This is assuredly incomplete and out of date...  
https://pulverizer.github.io/Movecraft-for-Sponge/
