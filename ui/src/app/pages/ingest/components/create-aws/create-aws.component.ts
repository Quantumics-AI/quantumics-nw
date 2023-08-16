import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';

@Component({
  selector: 'app-create-aws',
  templateUrl: './create-aws.component.html',
  styleUrls: ['./create-aws.component.css']
})
export class CreateAwsComponent {
  private certificate$: Observable<Certificate>;
  private certificateData: Certificate;
  private unsubscribe: Subject<void> = new Subject();
  private projectId: number;

  public fg: FormGroup;
  public loading: boolean;
  public queryFolders: boolean = true;
  public selectedPolicyName: string;
  public policyData: any = [
    {
      id: 1,
      policyName: "IAM",
    },
    {
      id: 2,
      policyName: "Resource Policy",
    },
    {
      id: 3,
      policyName: "Secret Key / Access Key"
    }
  ];
  public connection: boolean = false;

  constructor(
    private fb: FormBuilder,
    private snakbar: SnackbarService,
    private activatedRoute: ActivatedRoute,
    private quantumFacade: Quantumfacade,
    private router: Router,
  ){
    this.certificate$ = this.quantumFacade.certificate$;
    this.certificate$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(certificate => {
        if (certificate) {
          this.certificateData = certificate;
        }
      });
  }

  ngOnInit(): void {
    this.projectId = +this.activatedRoute.parent.snapshot.paramMap.get('projectId');

    this.fg = this.fb.group({
      folderName: new FormControl('', Validators.required),
      // markAsDefault: [false],
      policyName: new FormControl('', Validators.required),
      roleName: new FormControl('', Validators.required),
    });
  }


  public onSelectPipeline(connectorName: any): void {

  }

  public continue(): void {
    this.router.navigate([`projects/${this.projectId}/ingest/select-source-target`]);
  }

  public testConnection(): void {
    this.snakbar.open("Test connection succesfully");
    this.connection = true;
  }
}
