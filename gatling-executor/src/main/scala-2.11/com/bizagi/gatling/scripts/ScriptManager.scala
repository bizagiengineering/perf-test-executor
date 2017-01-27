package com.bizagi.gatling.scripts

import java.nio.file.Paths

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import scalaz.effect.IO

/**
  * Created by dev-williame on 1/3/17.
  */
object ScriptManager {

  def setup(uri: RepoUri, repoCredential: RepoCredential, repo: RepoPath): IO[RepoPath] =
    clone(uri, repoCredential, repo).map(_ => repo)

  def update(repoCredential: RepoCredential)(repo: RepoPath): IO[RepoPath] =
    if (repo.path.endsWith(".git"))
      pull(repoCredential, repo.path).map(_ => repo)
    else
      pull(repoCredential, Paths.get(repo.path).resolve(Paths.get(".git")).toString).map(_ => repo)

  private def clone(uri: RepoUri, repoCredential: RepoCredential, repo: RepoPath) =
    IO {
      Git.cloneRepository()
        .setCredentialsProvider(
          new UsernamePasswordCredentialsProvider(
            repoCredential.username.name,
            repoCredential.password.pass
          )
        )
        .setURI(uri.uri)
        .setDirectory(Paths.get(repo.path).toFile)
        .call()
    }

  private def pull(repoCredential: RepoCredential, path: String) =
    IO {
      new Git(new FileRepository(path))
        .pull()
        .setCredentialsProvider(
          new UsernamePasswordCredentialsProvider(repoCredential.username.name, repoCredential.password.pass)
        )
        .call()
    }
}

case class RepoPath(path: String) extends AnyVal
case class RepoCredential(username: UserName, password: Password)
case class UserName(name: String) extends AnyVal
case class Password(pass: String) extends AnyVal
case class RepoUri(uri: String) extends AnyVal
