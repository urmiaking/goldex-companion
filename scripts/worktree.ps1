<#
.SYNOPSIS
    Git Worktree Helper Script for GoldEx Companion agents and developers.

.DESCRIPTION
    Simplifies creating, listing, and cleaning up Git Worktrees for parallel feature development.

.EXAMPLE
    .\scripts\worktree.ps1 add custom-invoice
    Creates a new branch 'feature/custom-invoice' and worktree at '../goldex-custom-invoice'.

.EXAMPLE
    .\scripts\worktree.ps1 list
    Displays all currently active worktrees.

.EXAMPLE
    .\scripts\worktree.ps1 remove custom-invoice
    Removes the worktree '../goldex-custom-invoice' and cleans up.
#>

param (
    [Parameter(Position = 0, Mandatory = $true)]
    [ValidateSet("add", "list", "remove")]
    [string]$Action,

    [Parameter(Position = 1, Mandatory = $false)]
    [string]$Name
)

$RepoRoot = Resolve-Path "$PSScriptRoot\.."

switch ($Action) {
    "add" {
        if (-not $Name) {
            Write-Error "Please specify a feature name, e.g.: .\scripts\worktree.ps1 add my-feature"
            exit 1
        }
        $BranchName = if ($Name.StartsWith("feature/") -or $Name.StartsWith("fix/")) { $Name } else { "feature/$Name" }
        $CleanName = $Name -replace "[/\\:]", "-"
        $WorktreePath = "$RepoRoot\..\goldex-$CleanName"

        Write-Host "Creating worktree at: $WorktreePath" -ForegroundColor Cyan
        Write-Host "Branch: $BranchName" -ForegroundColor Cyan

        git -C "$RepoRoot" worktree add "$WorktreePath" -b "$BranchName" main
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Successfully created worktree! You can now open a new chat/IDE session in:" -ForegroundColor Green
            Write-Host "  $WorktreePath" -ForegroundColor Yellow
        }
    }

    "list" {
        git -C "$RepoRoot" worktree list
    }

    "remove" {
        if (-not $Name) {
            Write-Error "Please specify the feature name to remove, e.g.: .\scripts\worktree.ps1 remove my-feature"
            exit 1
        }
        $CleanName = $Name -replace "[/\\:]", "-"
        $WorktreePath = "$RepoRoot\..\goldex-$CleanName"

        Write-Host "Removing worktree: $WorktreePath" -ForegroundColor Yellow
        git -C "$RepoRoot" worktree remove "$WorktreePath"
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Worktree removed successfully." -ForegroundColor Green
        }
    }
}
