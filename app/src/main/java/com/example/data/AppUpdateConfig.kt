package com.example.data

object AppUpdateConfig {
    const val GITHUB_OWNER = "ESTRIN217"
    const val GITHUB_REPO = "secure-notes"

    val latestReleaseUrl: String
        get() = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    val releasesListUrl: String
        get() = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=100"

    val releasesWebUrl: String
        get() = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
}
