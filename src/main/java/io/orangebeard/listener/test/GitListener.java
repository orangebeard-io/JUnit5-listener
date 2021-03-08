package io.orangebeard.listener.test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;

//https://github.com/centic9/jgit-cookbook
public class GitListener {
    private final Repository repository;
    private final Git git;

    public GitListener(String dir) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();

        this.repository = builder.setGitDir(new File(dir))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
        this.git = new Git(repository);
        System.out.println("Current branch: " + repository.getFullBranch());

    }

    private void printDiffsBetweenCurrentBranchAndMaster() throws IOException, GitAPIException {
        AbstractTreeIterator currentBranch = prepareTreeParser(repository, repository.getFullBranch());
        AbstractTreeIterator master = prepareTreeParser(repository, "refs/heads/master");

        List<DiffEntry> diff = git.diff().setOldTree(master).setNewTree(currentBranch).call();
        for (DiffEntry entry : diff) {
            System.out.println("Entry: " + entry);
        }
    }

    private void printLocalDiffs() throws IOException, GitAPIException {
        AbstractTreeIterator currentRepository = new FileTreeIterator( git.getRepository() );
        AbstractTreeIterator committedChanges = prepareTreeParser(repository, repository.getFullBranch());

        List<DiffEntry> diff = git.diff().setOldTree(committedChanges).setNewTree(currentRepository).call();
        for (DiffEntry entry : diff) {
            System.out.println("Entry: " + entry);
        }

    }

    private AbstractTreeIterator prepareTreeParser(Repository repository, String ref) throws IOException {
        Ref head = repository.exactRef(ref);
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(head.getObjectId());
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }

    public static void main(String[] args) throws IOException, GitAPIException {
        GitListener gitListener = new GitListener("/home/maarten-jan/code/github-orangebeard/junit5-listener/.git");
        gitListener.printDiffsBetweenCurrentBranchAndMaster();
        gitListener.printLocalDiffs();
    }
}
