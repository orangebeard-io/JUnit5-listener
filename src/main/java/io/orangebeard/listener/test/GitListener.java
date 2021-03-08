package io.orangebeard.listener.test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.StatusCommand;
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

//https://github.com/centic9/jgit-cookbook
public class GitListener {

    public static void main(String[] args) throws IOException, GitAPIException {

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File("/home/maarten-jan/code/github-orangebeard/junit5-listener/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        Git git = new Git(repository);
        StatusCommand status = git.status();

        // the diff works on TreeIterators, we prepare two for the two branches
       // ObjectId oldTree = git.getRepository().resolve( "HEAD^{tree}" ); // equals newCommit.getTree()
        AbstractTreeIterator oldTreeParser = prepareTreeParser(repository, repository.getFullBranch());
        AbstractTreeIterator newTreeParser = prepareTreeParser(repository, "refs/heads/master");

// then the procelain diff-command returns a list of diff entries
        List<DiffEntry> diff = new Git(repository).diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).call();
        for(DiffEntry entry : diff) {
            System.out.println("Entry: " + entry.getNewPath());
        }

        for(String uncommitted: status.call().getUncommittedChanges()){
            System.out.println("uncommitted: "+ uncommitted);
        }
    }


    private static AbstractTreeIterator prepareTreeParser(Repository repository, String ref) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
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
}
